package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppRuleEntity
import com.example.data.LockMode
import com.example.data.LockSessionEntity
import com.example.data.LockStateCoordinator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name and verify partial shield and full lockdown interception`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aegis Lock", appName)

        val sampleRules = listOf(
            AppRuleEntity(
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                category = "VIDEO_STREAMING",
                isBlockedInPartial = true,
                isAllowedInFullLockdown = false
            ),
            AppRuleEntity(
                packageName = "com.google.android.dialer",
                appName = "Phone & Calls",
                category = "ESSENTIAL",
                isBlockedInPartial = false,
                isAllowedInFullLockdown = true,
                isCoreEssential = true
            )
        )

        val now = System.currentTimeMillis()
        val partialSession = LockSessionEntity(
            id = 1L,
            mode = LockMode.PARTIAL_SHIELD.name,
            startTimeEpochMs = now,
            endTimeEpochMs = now + 3600_000L,
            durationMinutes = 60,
            passcodeSha256 = LockStateCoordinator.sha256("654321"),
            unsealedPasscodeDisplay = "",
            isPasscodeSealed = true,
            guardianRecipientLabel = "Mom",
            blockSettingsAndUninstall = true,
            isActive = true
        )

        LockStateCoordinator.syncFromRepository(context, partialSession, sampleRules)

        // YouTube must be blocked in Partial Shield
        val ytIntercept = LockStateCoordinator.evaluatePackageInterception(context, "com.google.android.youtube")
        assertNotNull(ytIntercept)

        // Android Settings must be blocked when blockSettingsAndUninstall is true
        val settingsIntercept = LockStateCoordinator.evaluatePackageInterception(context, "com.android.settings")
        assertNotNull(settingsIntercept)

        // Unlisted utility app is allowed in Partial Shield
        val calcIntercept = LockStateCoordinator.evaluatePackageInterception(context, "com.google.android.calculator")
        assertNull(calcIntercept)

        // Switch to Full Lockdown
        val fullLockSession = partialSession.copy(mode = LockMode.FULL_LOCKDOWN.name)
        LockStateCoordinator.syncFromRepository(context, fullLockSession, sampleRules)

        // Unlisted app is blocked in Full Lockdown
        val fullCalcIntercept = LockStateCoordinator.evaluatePackageInterception(context, "com.google.android.calculator")
        assertNotNull(fullCalcIntercept)

        // Phone Dialer remains allowed in Full Lockdown
        val dialerIntercept = LockStateCoordinator.evaluatePackageInterception(context, "com.google.android.dialer")
        assertNull(dialerIntercept)
    }
}
