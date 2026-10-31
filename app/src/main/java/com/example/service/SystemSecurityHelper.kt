package com.example.service

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils

object SystemSecurityHelper {

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        if (AegisAccessibilityService.isServiceConnected.value) return true
        val expectedComponent = ComponentName(context, AegisAccessibilityService::class.java)
        val enabledServices = runCatching {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        }.getOrNull() ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponent) {
                return true
            }
        }
        return false
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        val adminComponent = ComponentName(context, AegisDeviceAdminReceiver::class.java)
        return runCatching { dpm.isAdminActive(adminComponent) }.getOrDefault(false)
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun requestDeviceAdminActivation(context: Context) {
        val adminComponent = ComponentName(context, AegisDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Prevents Aegis Lock from being uninstalled or bypassed while your Guardian Lock Timer is active."
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun launchEssentialDialer(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun launchEssentialMessaging(context: Context): Boolean {
        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(smsIntent)
            true
        }.getOrElse {
            val fallback = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MESSAGING)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching {
                context.startActivity(fallback)
                true
            }.getOrDefault(false)
        }
    }

    fun launchWhitelistedPackage(context: Context, packageName: String): Boolean {
        if (packageName.contains("dialer", ignoreCase = true)) {
            return launchEssentialDialer(context)
        }
        if (packageName.contains("messaging", ignoreCase = true) || packageName.contains("mms", ignoreCase = true)) {
            return launchEssentialMessaging(context)
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: return false
        return runCatching {
            context.startActivity(launchIntent)
            true
        }.getOrDefault(false)
    }
}
