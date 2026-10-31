package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.LockMode
import com.example.service.SystemSecurityHelper
import com.example.ui.AegisUiState
import com.example.ui.AegisViewModel
import com.example.ui.components.AegisLockShieldOverlay
import com.example.ui.components.DynamicIslandBar
import com.example.ui.screens.AppRulesScreen
import com.example.ui.screens.FortressCommandScreen
import com.example.ui.screens.HardeningSecurityScreen
import com.example.ui.screens.VaultAnalyticsScreen
import com.example.ui.theme.AmberGuardian
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated

enum class AegisDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    FORTRESS(
        route = "fortress",
        label = "Fortress",
        selectedIcon = Icons.Filled.Shield,
        unselectedIcon = Icons.Outlined.Shield
    ),
    APP_RULES(
        route = "app_rules",
        label = "App Rules",
        selectedIcon = Icons.Filled.Apps,
        unselectedIcon = Icons.Outlined.Apps
    ),
    HARDENING(
        route = "hardening",
        label = "Hardening",
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Outlined.Security
    ),
    VAULT_LOG(
        route = "vault_log",
        label = "Vault Log",
        selectedIcon = Icons.Filled.Analytics,
        unselectedIcon = Icons.Outlined.Analytics
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                AegisLockApp()
            }
        }
    }
}

@Composable
fun AegisLockApp(
    viewModel: AegisViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentDestination by rememberSaveable { mutableStateOf(AegisDestination.FORTRESS) }

    // Refresh Accessibility & Device Admin status whenever user returns from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // BackHandler for non-home tabs when Shield Overlay is not open
    BackHandler(enabled = !uiState.isShieldOverlayVisible && currentDestination != AegisDestination.FORTRESS) {
        currentDestination = AegisDestination.FORTRESS
    }

    // BackHandler absorbed when the Lock Shield Overlay is active
    BackHandler(enabled = uiState.isShieldOverlayVisible) {
        // Cannot back out of the Lock Shield without Guardian Passcode or Safe Return button
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050811),
                        ObsidianBackground,
                        Color(0xFF0A1320)
                    )
                )
            )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DynamicIslandBar(
                            activeSession = uiState.activeSession,
                            currentTimeMs = uiState.currentTimeMs,
                            onExpandActiveShield = {
                                viewModel.openShieldOverlay()
                            }
                        )

                        AnimatedVisibility(visible = uiState.statusBannerMessage != null) {
                            uiState.statusBannerMessage?.let { msg ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = ObsidianSurfaceElevated,
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp, vertical = 4.dp)
                                        .widthIn(max = 600.dp)
                                        .fillMaxWidth()
                                        .border(1.dp, AmberGuardian.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                        .clickable { viewModel.clearStatusBanner() }
                                        .testTag("status_banner")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AmberGuardian,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss Banner",
                                            tint = AmberGuardian
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    if (!isExpandedScreen) {
                        NavigationBar(
                            containerColor = ObsidianSurface.copy(alpha = 0.96f),
                            tonalElevation = 12.dp,
                            modifier = Modifier.testTag("bottom_navigation_bar")
                        ) {
                            for (dest in AegisDestination.entries) {
                                val selected = currentDestination == dest
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF032219),
                                        selectedTextColor = EmeraldPrimary,
                                        indicatorColor = EmeraldPrimary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${dest.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        NavigationRail(
                            containerColor = ObsidianSurface.copy(alpha = 0.95f),
                            modifier = Modifier
                                .fillMaxHeight()
                                .testTag("side_navigation_rail")
                        ) {
                            for (dest in AegisDestination.entries) {
                                val selected = currentDestination == dest
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentDestination = dest },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = Color(0xFF032219),
                                        selectedTextColor = EmeraldPrimary,
                                        indicatorColor = EmeraldPrimary
                                    ),
                                    modifier = Modifier.testTag("rail_tab_${dest.route}")
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 680.dp)
                                .fillMaxSize()
                        ) {
                            AnimatedContent(
                                targetState = currentDestination,
                                transitionSpec = {
                                    (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                        scaleIn(initialScale = 0.97f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                                        .togetherWith(
                                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                                        )
                                },
                                label = "main_tab_transition"
                            ) { targetTab ->
                                when (targetTab) {
                                    AegisDestination.FORTRESS -> {
                                        FortressCommandScreen(
                                            uiState = uiState,
                                            onSelectMode = viewModel::selectMode,
                                            onSelectDuration = viewModel::selectDurationMinutes,
                                            onRegeneratePasscode = viewModel::regenerateGuardianPasscode,
                                            onUpdateCustomPasscode = viewModel::updateCustomDraftPasscode,
                                            onUpdateGuardianRecipient = viewModel::updateGuardianRecipient,
                                            onToggleSealImmediately = viewModel::setSealPasscodeImmediately,
                                            onToggleBlockSettings = viewModel::setBlockSettingsAndUninstall,
                                            onActivateLock = viewModel::activateFortressLock,
                                            onSealActivePasscodeNow = viewModel::sealActivePasscodeNow,
                                            onOpenFullShieldOverlay = { viewModel.openShieldOverlay() },
                                            onSimulateBlockedAppLaunch = viewModel::triggerSimulatedInterception
                                        )
                                    }
                                    AegisDestination.APP_RULES -> {
                                        AppRulesScreen(
                                            uiState = uiState,
                                            onTogglePartialBlock = viewModel::toggleAppBlockedInPartial,
                                            onToggleFullWhitelist = viewModel::toggleAppAllowedInFullLockdown,
                                            onSimulateInterception = viewModel::triggerSimulatedInterception
                                        )
                                    }
                                    AegisDestination.HARDENING -> {
                                        HardeningSecurityScreen(
                                            uiState = uiState,
                                            onRefreshPermissions = viewModel::refreshSystemPermissions,
                                            onSimulateTamperAttempt = viewModel::triggerSimulatedInterception
                                        )
                                    }
                                    AegisDestination.VAULT_LOG -> {
                                        VaultAnalyticsScreen(uiState = uiState)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full-Screen Un-Bypassable Lock Shield Overlay
        AnimatedVisibility(
            visible = uiState.isShieldOverlayVisible,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
            exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                scaleOut(targetScale = 0.95f)
        ) {
            val activeSession = uiState.activeSession
            val activeMode = activeSession?.let {
                runCatching { LockMode.valueOf(it.mode) }.getOrDefault(uiState.selectedMode)
            } ?: uiState.selectedMode

            val remainingSec = activeSession?.remainingSeconds(uiState.currentTimeMs)
                ?: (uiState.selectedDurationMinutes * 60L)
            val progress = activeSession?.progressFraction(uiState.currentTimeMs) ?: 0.05f

            AegisLockShieldOverlay(
                mode = activeMode,
                remainingSeconds = remainingSec,
                progressFraction = progress,
                guardianLabel = activeSession?.guardianRecipientLabel ?: uiState.guardianRecipientLabel,
                isPasscodeSealed = activeSession?.isPasscodeSealed ?: uiState.sealPasscodeImmediately,
                interceptedAppName = uiState.overlayInterceptedAppName,
                interceptedReason = uiState.overlayInterceptedReason,
                onVerifyPasscode = { enteredCode ->
                    viewModel.verifyGuardianPasscodeAndUnlock(enteredCode)
                },
                onOpenDialer = {
                    SystemSecurityHelper.launchEssentialDialer(context)
                },
                onOpenMessaging = {
                    SystemSecurityHelper.launchEssentialMessaging(context)
                },
                onReturnToSafeArea = {
                    viewModel.dismissShieldOverlayToSafeHub()
                }
            )
        }
    }
}
