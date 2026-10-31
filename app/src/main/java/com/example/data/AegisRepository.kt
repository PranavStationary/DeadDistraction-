package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.SecureRandom

class AegisRepository(
    private val context: Context,
    private val dao: AegisDao
) {
    val allAppRules: Flow<List<AppRuleEntity>> = dao.observeAllAppRules()
    val activeSession: Flow<LockSessionEntity?> = dao.observeActiveSession()
    val sessionHistory: Flow<List<LockSessionEntity>> = dao.observeSessionHistory()
    val interceptionLogs: Flow<List<InterceptionLogEntity>> = dao.observeInterceptionLogs()

    /**
     * Initializes the curated Social Media + Essential Phone/SMS catalog and merges any real
     * launchable applications installed on the user's Android device via PackageManager.
     */
    suspend fun initializeCatalogAndSyncInstalledApps() = withContext(Dispatchers.IO) {
        val existingRules = dao.getAllAppRulesOnce().associateBy { it.packageName }
        val pm = context.packageManager

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val installedResolveInfos = runCatching {
            pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        }.getOrDefault(emptyList())

        val installedMap = mutableMapOf<String, String>()
        for (ri in installedResolveInfos) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == context.packageName) continue
            val label = runCatching { ri.loadLabel(pm).toString() }.getOrDefault(pkg)
            installedMap[pkg] = label
        }

        val defaultPresets = listOf(
            // Major Social Media & Video Distractions (Blocked in Partial Shield by default)
            AppRuleEntity(
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                category = AppCategory.VIDEO_STREAMING.name,
                isBlockedInPartial = true,
                isAllowedInFullLockdown = false,
                isCoreEssential = false,
                isInstalledOnDevice = installedMap.containsKey("com.google.android.youtube"),
                brandHexColor = 0xFFEF4444
            ),
            AppRuleEntity(
                packageName = "com.instagram.android",
                appName = "Instagram",
                category = AppCategory.SOCIAL_MEDIA.name,
                isBlockedInPartial = true,
                isAllowedInFullLockdown = false,
                isCoreEssential = false,
                isInstalledOnDevice = installedMap.containsKey("com.instagram.android"),
                brandHexColor = 0xFFEC4899
            ),
            AppRuleEntity(
                packageName = "com.facebook.katana",
                appName = "Facebook",
                category = AppCategory.SOCIAL_MEDIA.name,
                isBlockedInPartial = true,
                isAllowedInFullLockdown = false,
                isCoreEssential = false,
                isInstalledOnDevice = installedMap.containsKey("com.facebook.katana"),
                brandHexColor = 0xFF3B82F6
            ),
            AppRuleEntity(
                packageName = "com.zhiliaoapp.musically",
                appName = "TikTok",
                category = AppCategory.VIDEO_STREAMING.name,
                isBlockedInPartial = true,
                isAllowedInFullLockdown = false,
                isCoreEssential = false,
                isInstalledOnDevice = installedMap.containsKey("com.zhiliaoapp.musically"),
                brandHexColor = 0xFF06B6D4
            ),
            AppRuleEntity(
                packageName = "com.twitter.android",
                appName = "X (Twitter)",
                category = AppCategory.SOCIAL_MEDIA.name,
                isBlockedInPartial = true,
                isAllowedInFullLockdown = false,
                isCoreEssential = false,
                isInstalledOnDevice = installedMap.containsKey("com.twitter.android"),
                brandHexColor = 0xFF64748B
            ),
            AppRuleEntity(
                packageName = "com.snapchat.android",
                appName = "Snapchat",
                category = AppCategory.SOCIAL_MEDIA.name,
                isBlockedInPartial = true,
                isAllowedInFullLockdown = false,
                isCoreEssential = false,
                isInstalledOnDevice = installedMap.containsKey("com.snapchat.android"),
                brandHexColor = 0xFFFACC15
            ),
            AppRuleEntity(
                packageName = "com.reddit.frontpage",
                appName = "Reddit",
                category = AppCategory.SOCIAL_MEDIA.name,
                isBlockedInPartial = true,
                isAllowedInFullLockdown = false,
                isCoreEssential = false,
                isInstalledOnDevice = installedMap.containsKey("com.reddit.frontpage"),
                brandHexColor = 0xFFF97316
            ),
            AppRuleEntity(
                packageName = "com.netflix.mediaclient",
                appName = "Netflix",
                category = AppCategory.VIDEO_STREAMING.name,
                isBlockedInPartial = true,
                isAllowedInFullLockdown = false,
                isCoreEssential = false,
                isInstalledOnDevice = installedMap.containsKey("com.netflix.mediaclient"),
                brandHexColor = 0xFFDC2626
            ),
            AppRuleEntity(
                packageName = "com.discord",
                appName = "Discord",
                category = AppCategory.GAMING.name,
                isBlockedInPartial = true,
                isAllowedInFullLockdown = false,
                isCoreEssential = false,
                isInstalledOnDevice = installedMap.containsKey("com.discord"),
                brandHexColor = 0xFF6366F1
            ),
            AppRuleEntity(
                packageName = "com.android.chrome",
                appName = "Chrome Browser",
                category = AppCategory.UTILITY.name,
                isBlockedInPartial = false,
                isAllowedInFullLockdown = false,
                isCoreEssential = false,
                isInstalledOnDevice = installedMap.containsKey("com.android.chrome"),
                brandHexColor = 0xFFF59E0B
            ),
            // Essential Phone & Messaging Apps (Allowed in Full Mobile Lockdown by default)
            AppRuleEntity(
                packageName = "com.google.android.dialer",
                appName = "Phone & Calls",
                category = AppCategory.ESSENTIAL.name,
                isBlockedInPartial = false,
                isAllowedInFullLockdown = true,
                isCoreEssential = true,
                isInstalledOnDevice = true,
                brandHexColor = 0xFF10B981
            ),
            AppRuleEntity(
                packageName = "com.google.android.apps.messaging",
                appName = "Messages (SMS)",
                category = AppCategory.ESSENTIAL.name,
                isBlockedInPartial = false,
                isAllowedInFullLockdown = true,
                isCoreEssential = true,
                isInstalledOnDevice = true,
                brandHexColor = 0xFF06B6D4
            ),
            AppRuleEntity(
                packageName = "com.google.android.contacts",
                appName = "Contacts",
                category = AppCategory.ESSENTIAL.name,
                isBlockedInPartial = false,
                isAllowedInFullLockdown = true,
                isCoreEssential = true,
                isInstalledOnDevice = installedMap.containsKey("com.google.android.contacts"),
                brandHexColor = 0xFF14B8A6
            ),
            AppRuleEntity(
                packageName = "com.google.android.deskclock",
                appName = "Clock & Alarms",
                category = AppCategory.ESSENTIAL.name,
                isBlockedInPartial = false,
                isAllowedInFullLockdown = true,
                isCoreEssential = true,
                isInstalledOnDevice = installedMap.containsKey("com.google.android.deskclock"),
                brandHexColor = 0xFF8B5CF6
            )
        )

        val mergedList = mutableListOf<AppRuleEntity>()

        for (preset in defaultPresets) {
            val prev = existingRules[preset.packageName]
            if (prev != null) {
                mergedList.add(
                    prev.copy(isInstalledOnDevice = preset.isInstalledOnDevice || prev.isCoreEssential)
                )
            } else {
                mergedList.add(preset)
            }
        }

        // Also include any other real launchable apps installed on the device
        val presetPackages = defaultPresets.map { it.packageName }.toSet()
        for ((pkg, label) in installedMap) {
            if (pkg in presetPackages) continue
            val prev = existingRules[pkg]
            if (prev != null) {
                mergedList.add(prev.copy(appName = label, isInstalledOnDevice = true))
            } else {
                val isDialerOrSms = LockStateCoordinator.isCorePhoneOrSmsPackage(pkg) ||
                    pkg.contains("dialer", ignoreCase = true) ||
                    pkg.contains("messaging", ignoreCase = true) ||
                    pkg.contains("mms", ignoreCase = true)
                mergedList.add(
                    AppRuleEntity(
                        packageName = pkg,
                        appName = label,
                        category = if (isDialerOrSms) AppCategory.ESSENTIAL.name else AppCategory.UTILITY.name,
                        isBlockedInPartial = false,
                        isAllowedInFullLockdown = isDialerOrSms,
                        isCoreEssential = isDialerOrSms,
                        isInstalledOnDevice = true,
                        brandHexColor = if (isDialerOrSms) 0xFF10B981 else 0xFF64748B
                    )
                )
            }
        }

        dao.upsertAppRules(mergedList)
        refreshFastCoordinator()
    }

    suspend fun togglePartialBlock(rule: AppRuleEntity, blocked: Boolean) = withContext(Dispatchers.IO) {
        dao.updateAppRule(rule.copy(isBlockedInPartial = blocked))
        refreshFastCoordinator()
    }

    suspend fun toggleFullLockdownWhitelist(rule: AppRuleEntity, allowed: Boolean) = withContext(Dispatchers.IO) {
        dao.updateAppRule(rule.copy(isAllowedInFullLockdown = allowed))
        refreshFastCoordinator()
    }

    fun generateSecureGuardianPasscode(): String {
        val random = SecureRandom()
        val number = 100000 + random.nextInt(900000)
        return number.toString()
    }

    suspend fun startLockSession(
        mode: LockMode,
        durationMinutes: Int,
        rawPasscode: String,
        sealPasscodeImmediately: Boolean,
        guardianRecipientLabel: String,
        blockSettingsAndUninstall: Boolean
    ): LockSessionEntity = withContext(Dispatchers.IO) {
        // Complete any stale active session first
        val existing = dao.getActiveSessionOnce()
        if (existing != null) {
            dao.updateLockSession(existing.copy(isActive = false))
        }

        val now = System.currentTimeMillis()
        val endMs = now + durationMinutes * 60_000L
        val hash = LockStateCoordinator.sha256(rawPasscode)

        val newSession = LockSessionEntity(
            mode = mode.name,
            startTimeEpochMs = now,
            endTimeEpochMs = endMs,
            durationMinutes = durationMinutes,
            passcodeSha256 = hash,
            unsealedPasscodeDisplay = if (sealPasscodeImmediately) "" else rawPasscode,
            isPasscodeSealed = sealPasscodeImmediately,
            guardianRecipientLabel = guardianRecipientLabel.ifBlank { "Family Guardian" },
            blockSettingsAndUninstall = blockSettingsAndUninstall,
            isActive = true
        )
        val insertedId = dao.insertLockSession(newSession)
        val savedSession = newSession.copy(id = insertedId)
        refreshFastCoordinator()
        savedSession
    }

    /**
     * Seals and hides the passcode after the user hands it over to a family member,
     * erasing the plaintext passcode from the database so only the SHA-256 hash remains!
     */
    suspend fun sealActiveSessionPasscode() = withContext(Dispatchers.IO) {
        val current = dao.getActiveSessionOnce() ?: return@withContext
        dao.updateLockSession(
            current.copy(
                unsealedPasscodeDisplay = "",
                isPasscodeSealed = true
            )
        )
        refreshFastCoordinator()
    }

    /**
     * Verifies the 6-digit Guardian Passcode against the active session's SHA-256 hash.
     * If valid, unlocks the session immediately.
     */
    suspend fun verifyPasscodeAndUnlock(candidatePasscode: String): Boolean = withContext(Dispatchers.IO) {
        val current = dao.getActiveSessionOnce() ?: return@withContext true
        val candidateHash = LockStateCoordinator.sha256(candidatePasscode)
        if (candidateHash == current.passcodeSha256) {
            dao.updateLockSession(
                current.copy(
                    isActive = false,
                    unlockedWithPasscode = true,
                    unsealedPasscodeDisplay = ""
                )
            )
            refreshFastCoordinator()
            true
        } else {
            false
        }
    }

    /**
     * Checks if the active session's timer has naturally expired, and marks it completed if so.
     */
    suspend fun checkAndExpireTimerIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        val current = dao.getActiveSessionOnce() ?: return@withContext false
        if (System.currentTimeMillis() >= current.endTimeEpochMs) {
            dao.updateLockSession(
                current.copy(
                    isActive = false,
                    completedNaturally = true,
                    unsealedPasscodeDisplay = ""
                )
            )
            refreshFastCoordinator()
            true
        } else {
            false
        }
    }

    suspend fun recordInterceptionEvent(
        packageName: String,
        appName: String,
        reason: String
    ) = withContext(Dispatchers.IO) {
        val current = dao.getActiveSessionOnce()
        val sessionMode = current?.mode ?: LockMode.PARTIAL_SHIELD.name
        if (current != null) {
            dao.incrementSessionInterceptions(current.id)
        }
        dao.incrementAppBlockedCount(packageName)
        dao.insertInterceptionLog(
            InterceptionLogEntity(
                packageName = packageName,
                appName = appName,
                sessionMode = sessionMode,
                reason = reason
            )
        )
        LockStateCoordinator.recordInterceptionBanner(context, packageName, appName, reason)
    }

    suspend fun refreshFastCoordinator() = withContext(Dispatchers.IO) {
        val session = dao.getActiveSessionOnce()
        val rules = dao.getAllAppRulesOnce()
        LockStateCoordinator.syncFromRepository(context, session, rules)
    }
}
