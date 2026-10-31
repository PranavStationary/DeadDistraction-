package com.example.data

import android.content.Context
import java.security.MessageDigest

/**
 * Ultra-fast synchronous coordinator backed by SharedPreferences and in-memory state.
 * Used by [com.example.service.AegisAccessibilityService] and [com.example.service.LockInterceptorActivity]
 * to evaluate window state changes in < 1ms without blocking the main/accessibility thread.
 */
object LockStateCoordinator {
    private const val PREFS_NAME = "aegis_fast_lock_state"
    private const val KEY_IS_ACTIVE = "is_active"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_MODE = "mode"
    private const val KEY_START_MS = "start_ms"
    private const val KEY_END_MS = "end_ms"
    private const val KEY_PASSCODE_SHA256 = "passcode_sha256"
    private const val KEY_GUARDIAN_LABEL = "guardian_label"
    private const val KEY_IS_SEALED = "is_sealed"
    private const val KEY_BLOCK_SETTINGS = "block_settings"
    private const val KEY_PARTIAL_BLOCKED_PKGS = "partial_blocked_pkgs"
    private const val KEY_FULL_ALLOWED_PKGS = "full_allowed_pkgs"
    private const val KEY_LAST_INTERCEPTED_PKG = "last_intercepted_pkg"
    private const val KEY_LAST_INTERCEPTED_NAME = "last_intercepted_name"
    private const val KEY_LAST_INTERCEPTED_REASON = "last_intercepted_reason"

    data class FastLockSnapshot(
        val isActive: Boolean,
        val sessionId: Long,
        val mode: LockMode,
        val startTimeEpochMs: Long,
        val endTimeEpochMs: Long,
        val passcodeSha256: String,
        val guardianLabel: String,
        val isPasscodeSealed: Boolean,
        val blockSettingsAndUninstall: Boolean,
        val partialBlockedPackages: Set<String>,
        val fullAllowedPackages: Set<String>,
        val lastInterceptedPackage: String,
        val lastInterceptedAppName: String,
        val lastInterceptedReason: String
    )

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.trim().toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun syncFromRepository(
        context: Context,
        activeSession: LockSessionEntity?,
        allRules: List<AppRuleEntity>
    ) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val partialBlocked = allRules.filter { it.isBlockedInPartial }.map { it.packageName }.toSet()
        val fullAllowed = allRules.filter { it.isAllowedInFullLockdown }.map { it.packageName }.toSet()

        prefs.edit().apply {
            if (activeSession != null && activeSession.isActive && activeSession.endTimeEpochMs > System.currentTimeMillis()) {
                putBoolean(KEY_IS_ACTIVE, true)
                putLong(KEY_SESSION_ID, activeSession.id)
                putString(KEY_MODE, activeSession.mode)
                putLong(KEY_START_MS, activeSession.startTimeEpochMs)
                putLong(KEY_END_MS, activeSession.endTimeEpochMs)
                putString(KEY_PASSCODE_SHA256, activeSession.passcodeSha256)
                putString(KEY_GUARDIAN_LABEL, activeSession.guardianRecipientLabel)
                putBoolean(KEY_IS_SEALED, activeSession.isPasscodeSealed)
                putBoolean(KEY_BLOCK_SETTINGS, activeSession.blockSettingsAndUninstall)
            } else {
                putBoolean(KEY_IS_ACTIVE, false)
            }
            putStringSet(KEY_PARTIAL_BLOCKED_PKGS, partialBlocked)
            putStringSet(KEY_FULL_ALLOWED_PKGS, fullAllowed)
            apply()
        }
    }

    fun recordInterceptionBanner(
        context: Context,
        packageName: String,
        appName: String,
        reason: String
    ) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAST_INTERCEPTED_PKG, packageName)
            .putString(KEY_LAST_INTERCEPTED_NAME, appName)
            .putString(KEY_LAST_INTERCEPTED_REASON, reason)
            .apply()
    }

    fun readSnapshot(context: Context): FastLockSnapshot {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val endMs = prefs.getLong(KEY_END_MS, 0L)
        val rawActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
        val effectivelyActive = rawActive && endMs > now
        if (rawActive && !effectivelyActive) {
            prefs.edit().putBoolean(KEY_IS_ACTIVE, false).apply()
        }
        val modeStr = prefs.getString(KEY_MODE, LockMode.PARTIAL_SHIELD.name) ?: LockMode.PARTIAL_SHIELD.name
        val mode = runCatching { LockMode.valueOf(modeStr) }.getOrDefault(LockMode.PARTIAL_SHIELD)

        return FastLockSnapshot(
            isActive = effectivelyActive,
            sessionId = prefs.getLong(KEY_SESSION_ID, -1L),
            mode = mode,
            startTimeEpochMs = prefs.getLong(KEY_START_MS, 0L),
            endTimeEpochMs = endMs,
            passcodeSha256 = prefs.getString(KEY_PASSCODE_SHA256, "") ?: "",
            guardianLabel = prefs.getString(KEY_GUARDIAN_LABEL, "Family Guardian") ?: "Family Guardian",
            isPasscodeSealed = prefs.getBoolean(KEY_IS_SEALED, false),
            blockSettingsAndUninstall = prefs.getBoolean(KEY_BLOCK_SETTINGS, true),
            partialBlockedPackages = prefs.getStringSet(KEY_PARTIAL_BLOCKED_PKGS, emptySet()) ?: emptySet(),
            fullAllowedPackages = prefs.getStringSet(KEY_FULL_ALLOWED_PKGS, emptySet()) ?: emptySet(),
            lastInterceptedPackage = prefs.getString(KEY_LAST_INTERCEPTED_PKG, "") ?: "",
            lastInterceptedAppName = prefs.getString(KEY_LAST_INTERCEPTED_NAME, "Distracting App") ?: "Distracting App",
            lastInterceptedReason = prefs.getString(KEY_LAST_INTERCEPTED_REASON, "Active Distraction Shield") ?: "Active Distraction Shield"
        )
    }

    /**
     * Evaluates whether a foreground package must be intercepted right now.
     * Returns a non-null pair of (ReadableAppName, InterceptionReason) if blocked, or null if allowed.
     */
    fun evaluatePackageInterception(
        context: Context,
        foregroundPackage: String
    ): Pair<String, String>? {
        val snapshot = readSnapshot(context)
        if (!snapshot.isActive) return null

        val myPkg = context.packageName
        // Always allow our own app, SystemUI, and emergency telecom packages
        if (
            foregroundPackage == myPkg ||
            foregroundPackage == "com.android.systemui" ||
            foregroundPackage == "com.android.server.telecom" ||
            foregroundPackage == "com.android.incallui" ||
            foregroundPackage.contains("emergency", ignoreCase = true)
        ) {
            return null
        }

        // 1. Check Anti-Uninstall & Settings Tamper Guard
        val isSettingsOrUninstaller = foregroundPackage == "com.android.settings" ||
            foregroundPackage.contains("packageinstaller", ignoreCase = true) ||
            foregroundPackage == "com.google.android.packageinstaller"

        if (isSettingsOrUninstaller && snapshot.blockSettingsAndUninstall) {
            return "System Settings / Uninstaller" to "Anti-Bypass Guard: Settings & Uninstall locked until Guardian Passcode or Timer expires"
        }

        // 2. Evaluate by LockMode
        return when (snapshot.mode) {
            LockMode.PARTIAL_SHIELD -> {
                if (snapshot.partialBlockedPackages.contains(foregroundPackage)) {
                    val label = resolveFriendlyLabel(context, foregroundPackage)
                    label to "Blocked by Partial Distraction Shield"
                } else {
                    null
                }
            }
            LockMode.FULL_LOCKDOWN -> {
                // Allow default home launcher only if not launching a blocked app, OR allow whitelisted essential apps
                val isWhitelisted = snapshot.fullAllowedPackages.contains(foregroundPackage) ||
                    isCorePhoneOrSmsPackage(foregroundPackage)
                if (!isWhitelisted) {
                    val label = resolveFriendlyLabel(context, foregroundPackage)
                    label to "Blocked by Full Mobile Lockdown (Only Phone & SMS Allowed)"
                } else {
                    null
                }
            }
        }
    }

    fun isCorePhoneOrSmsPackage(pkg: String): Boolean {
        return pkg == "com.google.android.dialer" ||
            pkg == "com.android.dialer" ||
            pkg == "com.samsung.android.dialer" ||
            pkg == "com.google.android.apps.messaging" ||
            pkg == "com.android.mms" ||
            pkg == "com.samsung.android.messaging"
    }

    fun resolveFriendlyLabel(context: Context, packageName: String): String {
        knownCatalog[packageName]?.let { return it }
        return runCatching {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        }.getOrDefault(packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() })
    }

    private val knownCatalog = mapOf(
        "com.google.android.youtube" to "YouTube",
        "com.instagram.android" to "Instagram",
        "com.facebook.katana" to "Facebook",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.twitter.android" to "X (Twitter)",
        "com.snapchat.android" to "Snapchat",
        "com.reddit.frontpage" to "Reddit",
        "com.netflix.mediaclient" to "Netflix",
        "tv.twitch.android.app" to "Twitch",
        "com.discord" to "Discord",
        "com.whatsapp" to "WhatsApp",
        "com.android.chrome" to "Chrome Browser",
        "com.google.android.dialer" to "Phone Dialer",
        "com.google.android.apps.messaging" to "Messages (SMS)",
        "com.android.settings" to "System Settings"
    )
}
