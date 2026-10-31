package com.example.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.example.R
import com.example.data.LockStateCoordinator

class AegisDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val snapshot = LockStateCoordinator.readSnapshot(context)
        if (snapshot.isActive) {
            // Immediately launch LockInterceptorActivity if a lock session is active
            val lockIntent = Intent(context, LockInterceptorActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(LockInterceptorActivity.EXTRA_INTERCEPTED_PACKAGE, "com.android.settings")
                putExtra(LockInterceptorActivity.EXTRA_INTERCEPTED_APP_NAME, "Device Admin Guard")
                putExtra(
                    LockInterceptorActivity.EXTRA_INTERCEPTED_REASON,
                    "Anti-Uninstall Guard: Cannot revoke Device Admin while Guardian Timer is active."
                )
            }
            runCatching { context.startActivity(lockIntent) }
        }
        return context.getString(R.string.device_admin_disable_warning)
    }
}
