package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.data.AegisDatabase
import com.example.data.AegisRepository
import com.example.data.LockStateCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AegisAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastInterceptTimestampMs: Long = 0L
    private var lastInterceptedPkg: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceConnected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkgName = event?.packageName?.toString() ?: return
        if (pkgName.isBlank()) return

        // Ignore rapid duplicate events for the same package within 600ms
        val now = System.currentTimeMillis()
        if (pkgName == lastInterceptedPkg && (now - lastInterceptTimestampMs) < 600L) {
            return
        }

        val interception = LockStateCoordinator.evaluatePackageInterception(
            context = applicationContext,
            foregroundPackage = pkgName
        ) ?: return

        val (friendlyAppName, reason) = interception
        lastInterceptTimestampMs = now
        lastInterceptedPkg = pkgName

        LockStateCoordinator.recordInterceptionBanner(
            context = applicationContext,
            packageName = pkgName,
            appName = friendlyAppName,
            reason = reason
        )

        serviceScope.launch {
            val db = AegisDatabase.getInstance(applicationContext)
            val repo = AegisRepository(applicationContext, db.aegisDao())
            repo.recordInterceptionEvent(
                packageName = pkgName,
                appName = friendlyAppName,
                reason = reason
            )
        }

        // Launch the un-bypassable LockInterceptorActivity over the blocked app
        val lockIntent = Intent(applicationContext, LockInterceptorActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            putExtra(LockInterceptorActivity.EXTRA_INTERCEPTED_PACKAGE, pkgName)
            putExtra(LockInterceptorActivity.EXTRA_INTERCEPTED_APP_NAME, friendlyAppName)
            putExtra(LockInterceptorActivity.EXTRA_INTERCEPTED_REASON, reason)
        }
        startActivity(lockIntent)
    }

    override fun onInterrupt() {
        // Required override for AccessibilityService
    }

    override fun onUnbind(intent: Intent?): Boolean {
        _isServiceConnected.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        _isServiceConnected.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()
    }
}
