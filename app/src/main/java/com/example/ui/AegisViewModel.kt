package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AegisDatabase
import com.example.data.AegisRepository
import com.example.data.AppRuleEntity
import com.example.data.InterceptionLogEntity
import com.example.data.LockMode
import com.example.data.LockSessionEntity
import com.example.service.SystemSecurityHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AegisUiState(
    val appRules: List<AppRuleEntity> = emptyList(),
    val activeSession: LockSessionEntity? = null,
    val sessionHistory: List<LockSessionEntity> = emptyList(),
    val interceptionLogs: List<InterceptionLogEntity> = emptyList(),
    val currentTimeMs: Long = System.currentTimeMillis(),
    val selectedMode: LockMode = LockMode.PARTIAL_SHIELD,
    val selectedDurationMinutes: Int = 60,
    val draftPasscode: String = "482915",
    val guardianRecipientLabel: String = "Family Member",
    val sealPasscodeImmediately: Boolean = true,
    val blockSettingsAndUninstall: Boolean = true,
    val isAccessibilityEnabled: Boolean = false,
    val isDeviceAdminEnabled: Boolean = false,
    val isShieldOverlayVisible: Boolean = false,
    val overlayInterceptedAppName: String = "YouTube",
    val overlayInterceptedReason: String = "Blocked by Partial Distraction Shield",
    val statusBannerMessage: String? = null
)

class AegisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AegisRepository = AegisRepository(
        context = application.applicationContext,
        dao = AegisDatabase.getInstance(application.applicationContext).aegisDao()
    )

    private val _currentTimeMs = MutableStateFlow(System.currentTimeMillis())
    private val _selectedMode = MutableStateFlow(LockMode.PARTIAL_SHIELD)
    private val _selectedDurationMinutes = MutableStateFlow(60)
    private val _draftPasscode = MutableStateFlow(repository.generateSecureGuardianPasscode())
    private val _guardianRecipientLabel = MutableStateFlow("Family Guardian")
    private val _sealPasscodeImmediately = MutableStateFlow(true)
    private val _blockSettingsAndUninstall = MutableStateFlow(true)
    private val _isAccessibilityEnabled = MutableStateFlow(false)
    private val _isDeviceAdminEnabled = MutableStateFlow(false)
    private val _isShieldOverlayVisible = MutableStateFlow(false)
    private val _overlayInterceptedApp = MutableStateFlow("YouTube" to "Blocked by Partial Distraction Shield")
    private val _statusBannerMessage = MutableStateFlow<String?>(null)

    private data class ConfigSlice(
        val currentTimeMs: Long,
        val selectedMode: LockMode,
        val selectedDurationMinutes: Int,
        val draftPasscode: String,
        val guardianRecipientLabel: String,
        val sealPasscodeImmediately: Boolean,
        val blockSettingsAndUninstall: Boolean,
        val isAccessibilityEnabled: Boolean,
        val isDeviceAdminEnabled: Boolean,
        val isShieldOverlayVisible: Boolean,
        val overlayInterceptedApp: Pair<String, String>,
        val statusBannerMessage: String?
    )

    private val configSliceFlow = combine(
        _currentTimeMs,
        _selectedMode,
        _selectedDurationMinutes,
        _draftPasscode,
        _guardianRecipientLabel
    ) { time, mode, duration, code, guardian ->
        arrayOf(time, mode, duration, code, guardian)
    }.combine(
        combine(
            _sealPasscodeImmediately,
            _blockSettingsAndUninstall,
            _isAccessibilityEnabled,
            _isDeviceAdminEnabled,
            _isShieldOverlayVisible
        ) { seal, blockSettings, acc, admin, overlay ->
            arrayOf(seal, blockSettings, acc, admin, overlay)
        }
    ) { first, second ->
        first to second
    }.combine(
        combine(_overlayInterceptedApp, _statusBannerMessage) { pair, msg -> pair to msg }
    ) { (first, second), (pair, msg) ->
        ConfigSlice(
            currentTimeMs = first[0] as Long,
            selectedMode = first[1] as LockMode,
            selectedDurationMinutes = first[2] as Int,
            draftPasscode = first[3] as String,
            guardianRecipientLabel = first[4] as String,
            sealPasscodeImmediately = second[0] as Boolean,
            blockSettingsAndUninstall = second[1] as Boolean,
            isAccessibilityEnabled = second[2] as Boolean,
            isDeviceAdminEnabled = second[3] as Boolean,
            isShieldOverlayVisible = second[4] as Boolean,
            overlayInterceptedApp = pair,
            statusBannerMessage = msg
        )
    }

    val uiState: StateFlow<AegisUiState> = combine(
        repository.allAppRules,
        repository.activeSession,
        repository.sessionHistory,
        repository.interceptionLogs,
        configSliceFlow
    ) { rules, activeSession, history, logs, cfg ->
        AegisUiState(
            appRules = rules,
            activeSession = activeSession,
            sessionHistory = history,
            interceptionLogs = logs,
            currentTimeMs = cfg.currentTimeMs,
            selectedMode = cfg.selectedMode,
            selectedDurationMinutes = cfg.selectedDurationMinutes,
            draftPasscode = cfg.draftPasscode,
            guardianRecipientLabel = cfg.guardianRecipientLabel,
            sealPasscodeImmediately = cfg.sealPasscodeImmediately,
            blockSettingsAndUninstall = cfg.blockSettingsAndUninstall,
            isAccessibilityEnabled = cfg.isAccessibilityEnabled,
            isDeviceAdminEnabled = cfg.isDeviceAdminEnabled,
            isShieldOverlayVisible = cfg.isShieldOverlayVisible,
            overlayInterceptedAppName = cfg.overlayInterceptedApp.first,
            overlayInterceptedReason = cfg.overlayInterceptedApp.second,
            statusBannerMessage = cfg.statusBannerMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AegisUiState(draftPasscode = _draftPasscode.value)
    )

    init {
        viewModelScope.launch {
            repository.initializeCatalogAndSyncInstalledApps()
            refreshSystemPermissions()
        }
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _currentTimeMs.value = System.currentTimeMillis()
                val expired = repository.checkAndExpireTimerIfNeeded()
                if (expired) {
                    _isShieldOverlayVisible.value = false
                    _statusBannerMessage.value = "Focus timer completed! All apps are now unlocked."
                }
            }
        }
    }

    fun refreshSystemPermissions() {
        val ctx = getApplication<Application>().applicationContext
        _isAccessibilityEnabled.value = SystemSecurityHelper.isAccessibilityServiceEnabled(ctx)
        _isDeviceAdminEnabled.value = SystemSecurityHelper.isDeviceAdminActive(ctx)
    }

    fun selectMode(mode: LockMode) {
        _selectedMode.value = mode
    }

    fun selectDurationMinutes(minutes: Int) {
        _selectedDurationMinutes.value = minutes.coerceIn(5, 2880)
    }

    fun regenerateGuardianPasscode() {
        _draftPasscode.value = repository.generateSecureGuardianPasscode()
    }

    fun updateCustomDraftPasscode(digitsOnly: String) {
        val cleaned = digitsOnly.filter { it.isDigit() }.take(6)
        if (cleaned.isNotEmpty()) {
            _draftPasscode.value = cleaned
        }
    }

    fun updateGuardianRecipient(label: String) {
        _guardianRecipientLabel.value = label
    }

    fun setSealPasscodeImmediately(seal: Boolean) {
        _sealPasscodeImmediately.value = seal
    }

    fun setBlockSettingsAndUninstall(enabled: Boolean) {
        // Cannot disable anti-tamper guard while a lock session is active!
        if (uiState.value.activeSession?.isActive == true && !enabled) {
            _statusBannerMessage.value = "Cannot disable Anti-Bypass Guard while a lock timer is active!"
            return
        }
        _blockSettingsAndUninstall.value = enabled
    }

    fun toggleAppBlockedInPartial(rule: AppRuleEntity, blocked: Boolean) {
        if (uiState.value.activeSession?.isActive == true && !blocked) {
            _statusBannerMessage.value = "Active Lock Rule: Cannot unblock ${rule.appName} until timer expires or Guardian Passcode is entered."
            return
        }
        viewModelScope.launch {
            repository.togglePartialBlock(rule, blocked)
        }
    }

    fun toggleAppAllowedInFullLockdown(rule: AppRuleEntity, allowed: Boolean) {
        if (uiState.value.activeSession?.isActive == true && allowed) {
            _statusBannerMessage.value = "Active Lock Rule: Cannot whitelist new apps during an active Full Lockdown."
            return
        }
        viewModelScope.launch {
            repository.toggleFullLockdownWhitelist(rule, allowed)
        }
    }

    fun activateFortressLock() {
        val rawCode = _draftPasscode.value.padEnd(6, '0').take(6)
        val mode = _selectedMode.value
        val minutes = _selectedDurationMinutes.value
        val sealNow = _sealPasscodeImmediately.value
        val guardian = _guardianRecipientLabel.value.ifBlank { "Family Guardian" }
        val blockSettings = _blockSettingsAndUninstall.value

        viewModelScope.launch {
            repository.startLockSession(
                mode = mode,
                durationMinutes = minutes,
                rawPasscode = rawCode,
                sealPasscodeImmediately = sealNow,
                guardianRecipientLabel = guardian,
                blockSettingsAndUninstall = blockSettings
            )
            // Regenerate draft passcode so the previous one is not sitting in draft state
            _draftPasscode.value = repository.generateSecureGuardianPasscode()

            if (mode == LockMode.FULL_LOCKDOWN) {
                _overlayInterceptedApp.value = "Full Mobile Lockdown" to "All non-essential apps are locked. Only Phone Calls & SMS are accessible."
                _isShieldOverlayVisible.value = true
            } else {
                _statusBannerMessage.value = "Partial Distraction Shield engaged for $minutes min."
            }
        }
    }

    fun sealActivePasscodeNow() {
        viewModelScope.launch {
            repository.sealActiveSessionPasscode()
            _statusBannerMessage.value = "Guardian Passcode sealed & erased from view. Only your family guardian can unlock early."
        }
    }

    fun triggerSimulatedInterception(appName: String, packageName: String, reason: String) {
        viewModelScope.launch {
            repository.recordInterceptionEvent(
                packageName = packageName,
                appName = appName,
                reason = reason
            )
            _overlayInterceptedApp.value = appName to reason
            _isShieldOverlayVisible.value = true
        }
    }

    fun openShieldOverlay(appName: String = "Active Lock Shield", reason: String = "Protected by Aegis Guardian Timer") {
        _overlayInterceptedApp.value = appName to reason
        _isShieldOverlayVisible.value = true
    }

    fun dismissShieldOverlayToSafeHub() {
        _isShieldOverlayVisible.value = false
    }

    suspend fun verifyGuardianPasscodeAndUnlock(enteredCode: String): Boolean {
        val unlocked = repository.verifyPasscodeAndUnlock(enteredCode)
        if (unlocked) {
            _isShieldOverlayVisible.value = false
            _statusBannerMessage.value = "Guardian Passcode verified. Device & apps unlocked."
        }
        return unlocked
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }
}
