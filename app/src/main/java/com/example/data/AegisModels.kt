package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LockMode(
    val title: String,
    val subtitle: String,
    val badgeLabel: String
) {
    PARTIAL_SHIELD(
        title = "Partial Distraction Shield",
        subtitle = "Blocks selected social media & entertainment apps (YouTube, Instagram, Facebook, etc.) while keeping the rest of your phone unlocked.",
        badgeLabel = "SOCIAL SHIELD"
    ),
    FULL_LOCKDOWN(
        title = "Full Mobile Lockdown",
        subtitle = "Completely locks down your entire phone except essential communication apps (Phone Dialer, SMS Messages, Emergency) until timer or Guardian Passcode.",
        badgeLabel = "FULL LOCKDOWN"
    )
}

enum class AppCategory(val displayName: String) {
    SOCIAL_MEDIA("Social Media"),
    VIDEO_STREAMING("Video & Entertainment"),
    GAMING("Gaming & Feeds"),
    ESSENTIAL("Essential Phone & SMS"),
    UTILITY("Productivity & System")
}

@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: String,
    val isBlockedInPartial: Boolean,
    val isAllowedInFullLockdown: Boolean,
    val isCoreEssential: Boolean = false,
    val isInstalledOnDevice: Boolean = true,
    val blockedAttemptsCount: Int = 0,
    val brandHexColor: Long = 0xFF10B981
)

@Entity(tableName = "lock_sessions")
data class LockSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String, // LockMode.name
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long,
    val durationMinutes: Int,
    val passcodeSha256: String,
    val unsealedPasscodeDisplay: String, // Empty string once sealed for family member!
    val isPasscodeSealed: Boolean,
    val guardianRecipientLabel: String,
    val blockSettingsAndUninstall: Boolean,
    val isActive: Boolean,
    val completedNaturally: Boolean = false,
    val unlockedWithPasscode: Boolean = false,
    val interceptionsCount: Int = 0
) {
    fun remainingSeconds(nowEpochMs: Long = System.currentTimeMillis()): Long {
        if (!isActive) return 0L
        return ((endTimeEpochMs - nowEpochMs) / 1000L).coerceAtLeast(0L)
    }

    fun progressFraction(nowEpochMs: Long = System.currentTimeMillis()): Float {
        val totalMs = (endTimeEpochMs - startTimeEpochMs).coerceAtLeast(1L).toFloat()
        val elapsedMs = (nowEpochMs - startTimeEpochMs).coerceIn(0L, endTimeEpochMs - startTimeEpochMs).toFloat()
        return (elapsedMs / totalMs).coerceIn(0f, 1f)
    }
}

@Entity(tableName = "interception_logs")
data class InterceptionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val sessionMode: String,
    val reason: String
)
