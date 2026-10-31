package com.example.service

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.MainActivity
import com.example.data.AegisDatabase
import com.example.data.AegisRepository
import com.example.data.LockMode
import com.example.data.LockStateCoordinator
import com.example.ui.components.AegisLockShieldOverlay
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LockInterceptorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialAppName = intent?.getStringExtra(EXTRA_INTERCEPTED_APP_NAME)
        val initialReason = intent?.getStringExtra(EXTRA_INTERCEPTED_REASON)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                val scope = rememberCoroutineScope()
                val db = remember { AegisDatabase.getInstance(applicationContext) }
                val repository = remember { AegisRepository(applicationContext, db.aegisDao()) }

                var snapshot by remember { mutableStateOf(LockStateCoordinator.readSnapshot(applicationContext)) }
                var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

                // Hardware Back button is absorbed so the user cannot bypass the Lock Shield!
                BackHandler(enabled = true) {
                    // Absorb back press during active lock interception
                }

                LaunchedEffect(Unit) {
                    while (true) {
                        delay(500L)
                        nowMs = System.currentTimeMillis()
                        val expired = repository.checkAndExpireTimerIfNeeded()
                        snapshot = LockStateCoordinator.readSnapshot(applicationContext)
                        if (expired || !snapshot.isActive) {
                            finish()
                            break
                        }
                    }
                }

                val remainingSec = ((snapshot.endTimeEpochMs - nowMs) / 1000L).coerceAtLeast(0L)
                val totalSec = ((snapshot.endTimeEpochMs - snapshot.startTimeEpochMs) / 1000L).coerceAtLeast(1L)
                val progress = (1f - (remainingSec.toFloat() / totalSec.toFloat())).coerceIn(0f, 1f)

                AegisLockShieldOverlay(
                    mode = snapshot.mode,
                    remainingSeconds = remainingSec,
                    progressFraction = progress,
                    guardianLabel = snapshot.guardianLabel,
                    isPasscodeSealed = snapshot.isPasscodeSealed,
                    interceptedAppName = initialAppName ?: snapshot.lastInterceptedAppName,
                    interceptedReason = initialReason ?: snapshot.lastInterceptedReason,
                    onVerifyPasscode = { code ->
                        val unlocked = repository.verifyPasscodeAndUnlock(code)
                        if (unlocked) {
                            finish()
                        }
                        unlocked
                    },
                    onOpenDialer = {
                        SystemSecurityHelper.launchEssentialDialer(this@LockInterceptorActivity)
                    },
                    onOpenMessaging = {
                        SystemSecurityHelper.launchEssentialMessaging(this@LockInterceptorActivity)
                    },
                    onReturnToSafeArea = {
                        if (snapshot.mode == LockMode.PARTIAL_SHIELD) {
                            // Navigate to Android Home screen away from the blocked social media app
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            runCatching { startActivity(homeIntent) }
                            finish()
                        } else {
                            // In Full Lockdown, return to Aegis Lock safe hub
                            val hubIntent = Intent(this@LockInterceptorActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            runCatching { startActivity(hubIntent) }
                            finish()
                        }
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_INTERCEPTED_PACKAGE = "extra_intercepted_package"
        const val EXTRA_INTERCEPTED_APP_NAME = "extra_intercepted_app_name"
        const val EXTRA_INTERCEPTED_REASON = "extra_intercepted_reason"
    }
}
