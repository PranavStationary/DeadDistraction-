package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LockMode
import com.example.ui.AegisUiState
import com.example.ui.components.formatDurationHhMmSs
import com.example.ui.theme.AmberGuardian
import com.example.ui.theme.CrimsonLock
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FortressCommandScreen(
    uiState: AegisUiState,
    onSelectMode: (LockMode) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onRegeneratePasscode: () -> Unit,
    onUpdateCustomPasscode: (String) -> Unit,
    onUpdateGuardianRecipient: (String) -> Unit,
    onToggleSealImmediately: (Boolean) -> Unit,
    onToggleBlockSettings: (Boolean) -> Unit,
    onActivateLock: () -> Unit,
    onSealActivePasscodeNow: () -> Unit,
    onOpenFullShieldOverlay: () -> Unit,
    onSimulateBlockedAppLaunch: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val activeSession = uiState.activeSession
    val isLocked = activeSession?.isActive == true

    var showCustomPasscodeEditor by remember { mutableStateOf(false) }
    var copiedFeedback by remember { mutableStateOf(false) }

    val blockedPartialCount = uiState.appRules.count { it.isBlockedInPartial }
    val allowedFullCount = uiState.appRules.count { it.isAllowedInFullLockdown }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. iOS-Styled Segmented Mode Switcher: Partial Distraction vs Full Phone Lockdown
        IosModeSegmentedControl(
            selectedMode = if (isLocked) {
                runCatching { LockMode.valueOf(activeSession!!.mode) }.getOrDefault(uiState.selectedMode)
            } else {
                uiState.selectedMode
            },
            enabled = !isLocked,
            onModeSelected = { mode ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectMode(mode)
            }
        )

        // 2. Active Session Card OR Configuration Dial
        AnimatedContent(
            targetState = isLocked,
            label = "fortress_state_transition"
        ) { locked ->
            if (locked && activeSession != null) {
                ActiveFortressSessionCard(
                    session = activeSession,
                    currentTimeMs = uiState.currentTimeMs,
                    onSealPasscodeNow = onSealActivePasscodeNow,
                    onOpenFullShield = onOpenFullShieldOverlay,
                    onSimulateBlockedApp = onSimulateBlockedAppLaunch
                )
            } else {
                // Pre-Lock Configuration Panel
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    // Mode Overview & Timer Dial Card
                    val modeAccent by animateColorAsState(
                        targetValue = if (uiState.selectedMode == LockMode.FULL_LOCKDOWN) CrimsonLock else EmeraldPrimary,
                        label = "mode_accent"
                    )

                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = ObsidianSurface,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, modeAccent.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.selectedMode == LockMode.FULL_LOCKDOWN) {
                                        Icons.Default.PhonelinkLock
                                    } else {
                                        Icons.Default.Shield
                                    },
                                    contentDescription = uiState.selectedMode.title,
                                    tint = modeAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = uiState.selectedMode.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.selectedMode.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Interactive Circular Timer Ring Preview
                            Box(
                                modifier = Modifier.size(190.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val durationFraction = (uiState.selectedDurationMinutes.toFloat() / 480f).coerceIn(0.08f, 1f)
                                val animatedSweep by animateFloatAsState(
                                    targetValue = durationFraction * 360f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "dial_sweep"
                                )

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val stroke = 14.dp.toPx()
                                    val diameter = size.minDimension - stroke * 2
                                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

                                    drawArc(
                                        color = Color.White.copy(alpha = 0.07f),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = Size(diameter, diameter),
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                    )

                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(modeAccent, CyanPulse, modeAccent)
                                        ),
                                        startAngle = -90f,
                                        sweepAngle = animatedSweep,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = Size(diameter, diameter),
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = formatMinutesHumanReadable(uiState.selectedDurationMinutes),
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontSize = 34.sp
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.testTag("selected_duration_text")
                                    )
                                    Text(
                                        text = if (uiState.selectedMode == LockMode.PARTIAL_SHIELD) {
                                            "$blockedPartialCount Social Apps Blocked"
                                        } else {
                                            "Only $allowedFullCount Essential Apps Active"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = modeAccent
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Stepper +/- 15m buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectDuration((uiState.selectedDurationMinutes - 15).coerceAtLeast(5))
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier.testTag("duration_minus_button")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease 15m", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("15m")
                                }

                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectDuration((uiState.selectedDurationMinutes + 15).coerceAtMost(2880))
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier.testTag("duration_plus_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase 15m", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("15m")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Duration Preset Chips
                            val presets = listOf(
                                15 to "15m",
                                30 to "30m",
                                60 to "1h",
                                120 to "2h",
                                240 to "4h",
                                480 to "8h",
                                1440 to "24h"
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for ((mins, label) in presets) {
                                    val selected = uiState.selectedDurationMinutes == mins
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectDuration(mins)
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                fontFamily = JetBrainsMonoFamily,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = modeAccent.copy(alpha = 0.22f),
                                            selectedLabelColor = modeAccent
                                        ),
                                        modifier = Modifier.testTag("duration_preset_$label")
                                    )
                                }
                            }
                        }
                    }

                    // 3. Guardian Passcode Generator & Family Member Handover Card
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = ObsidianSurface,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AmberGuardian.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(AmberGuardian.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = "Guardian Key",
                                            tint = AmberGuardian
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Guardian Passcode Vault",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Give this passcode to a family member before locking",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        copiedFeedback = false
                                        onRegeneratePasscode()
                                    },
                                    modifier = Modifier.testTag("regenerate_passcode_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Regenerate Passcode",
                                        tint = AmberGuardian
                                    )
                                }
                            }

                            // 6-Digit Display Capsules
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("generated_passcode_display"),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                            ) {
                                val paddedCode = uiState.draftPasscode.padEnd(6, '0').take(6)
                                for (ch in paddedCode) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 44.dp, height = 54.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(ObsidianSurfaceElevated)
                                            .border(1.dp, AmberGuardian.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ch.toString(),
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontFamily = JetBrainsMonoFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = AmberGuardian
                                        )
                                    }
                                }
                            }

                            // Copy & Share to Family Member Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(
                                            ClipData.newPlainText("Aegis Guardian Passcode", uiState.draftPasscode)
                                        )
                                        copiedFeedback = true
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("copy_passcode_button")
                                ) {
                                    Icon(
                                        imageVector = if (copiedFeedback) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                                        contentDescription = "Copy Passcode",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (copiedFeedback) EmeraldPrimary else AmberGuardian
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (copiedFeedback) "Copied!" else "Copy Code",
                                        color = Color.White
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Aegis Lock Guardian Passcode: ${uiState.draftPasscode}\nKeep this secret! Only this code can unlock my phone before the focus timer finishes."
                                            )
                                        }
                                        runCatching {
                                            context.startActivity(Intent.createChooser(shareIntent, "Give Passcode to Family Guardian"))
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("share_passcode_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Hand Over to Family",
                                        modifier = Modifier.size(16.dp),
                                        tint = CyanPulse
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Family Handover", color = Color.White)
                                }
                            }

                            // Optional: Let family member type their own custom 6-digit code
                            Text(
                                text = if (showCustomPasscodeEditor) "Hide Custom Family Input" else "Or let a family member type their own secret 6-digit code",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberGuardian,
                                modifier = Modifier
                                    .clickable { showCustomPasscodeEditor = !showCustomPasscodeEditor }
                                    .padding(vertical = 4.dp)
                                    .testTag("toggle_custom_passcode_editor")
                            )

                            AnimatedVisibility(visible = showCustomPasscodeEditor) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = uiState.guardianRecipientLabel,
                                        onValueChange = onUpdateGuardianRecipient,
                                        label = { Text("Guardian Name (e.g., Mom, Dad, Partner)") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("guardian_name_input")
                                    )
                                    OutlinedTextField(
                                        value = uiState.draftPasscode,
                                        onValueChange = onUpdateCustomPasscode,
                                        label = { Text("Custom 6-Digit Guardian Passcode") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("custom_passcode_input")
                                    )
                                }
                            }

                            // Seal & Hide Passcode Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ObsidianSurfaceElevated.copy(alpha = 0.65f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Seal & Erase Passcode From My View",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Once locked, the code is SHA-256 hashed so you cannot view it—only your family guardian can unlock early.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.sealPasscodeImmediately,
                                    onCheckedChange = onToggleSealImmediately,
                                    colors = SwitchDefaults.colors(checkedThumbColor = AmberGuardian),
                                    modifier = Modifier.testTag("seal_passcode_switch")
                                )
                            }

                            // Anti-Uninstall & Settings Tamper Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ObsidianSurfaceElevated.copy(alpha = 0.65f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Anti-Bypass & Anti-Uninstall Shield",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Blocks Android Settings & Package Uninstaller while timer is running so there is zero escape.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.blockSettingsAndUninstall,
                                    onCheckedChange = onToggleBlockSettings,
                                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary),
                                    modifier = Modifier.testTag("anti_bypass_switch")
                                )
                            }
                        }
                    }

                    // 4. Primary Engage Fortress CTA Button
                    val ctaColor = if (uiState.selectedMode == LockMode.FULL_LOCKDOWN) CrimsonLock else EmeraldPrimary
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onActivateLock()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ctaColor,
                            contentColor = if (uiState.selectedMode == LockMode.FULL_LOCKDOWN) Color.White else Color(0xFF032219)
                        ),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .testTag("activate_fortress_lock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Engage Lock",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (uiState.selectedMode == LockMode.FULL_LOCKDOWN) {
                                "Engage Full Mobile Lockdown (${formatMinutesHumanReadable(uiState.selectedDurationMinutes)})"
                            } else {
                                "Engage Partial Social Media Shield (${formatMinutesHumanReadable(uiState.selectedDurationMinutes)})"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun IosModeSegmentedControl(
    selectedMode: LockMode,
    enabled: Boolean,
    onModeSelected: (LockMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (mode in LockMode.entries) {
                val isSelected = selectedMode == mode
                val pillColor by animateColorAsState(
                    targetValue = when {
                        !isSelected -> Color.Transparent
                        mode == LockMode.FULL_LOCKDOWN -> CrimsonLock
                        else -> EmeraldPrimary
                    },
                    label = "seg_pill_${mode.name}"
                )
                val textColor by animateColorAsState(
                    targetValue = when {
                        !isSelected -> MaterialTheme.colorScheme.onSurfaceVariant
                        mode == LockMode.FULL_LOCKDOWN -> Color.White
                        else -> Color(0xFF04261C)
                    },
                    label = "seg_text_${mode.name}"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.97f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "seg_scale_${mode.name}"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .scale(scale)
                        .clip(RoundedCornerShape(16.dp))
                        .background(pillColor)
                        .clickable(enabled = enabled) { onModeSelected(mode) }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                        .testTag("mode_tab_${mode.name}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (mode == LockMode.PARTIAL_SHIELD) "Partial App Shield" else "Full Phone Lockdown",
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveFortressSessionCard(
    session: com.example.data.LockSessionEntity,
    currentTimeMs: Long,
    onSealPasscodeNow: () -> Unit,
    onOpenFullShield: () -> Unit,
    onSimulateBlockedApp: (String, String, String) -> Unit
) {
    val mode = runCatching { LockMode.valueOf(session.mode) }.getOrDefault(LockMode.PARTIAL_SHIELD)
    val accent = if (mode == LockMode.FULL_LOCKDOWN) CrimsonLock else EmeraldPrimary
    val remainingSec = session.remainingSeconds(currentTimeMs)
    val progress = session.progressFraction(currentTimeMs)

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = ObsidianSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
            .testTag("active_session_card")
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.16f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GppGood,
                        contentDescription = "Active Protection",
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${mode.badgeLabel} ENGAGED • NO BYPASS",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = formatDurationHhMmSs(remainingSec),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 44.sp
                ),
                color = Color.White,
                modifier = Modifier.testTag("active_session_timer")
            )

            Text(
                text = "Guardian: ${session.guardianRecipientLabel} • Blocked Attempts: ${session.interceptionsCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // If passcode is still unsealed, allow sealing it now after handing over to family member
            if (!session.isPasscodeSealed && session.unsealedPasscodeDisplay.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AmberGuardian.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Unsealed Guardian Passcode: ${session.unsealedPasscodeDisplay}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = AmberGuardian
                        )
                        Text(
                            text = "Give this code to ${session.guardianRecipientLabel}, then tap below to permanently hide it from yourself.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onSealPasscodeNow,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberGuardian,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("seal_active_passcode_button")
                        ) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = "Seal Code", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Seal & Hide Passcode Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurfaceElevated.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Sealed",
                            tint = AmberGuardian
                        )
                        Text(
                            text = "Passcode is sealed & hidden. Only ${session.guardianRecipientLabel} can unlock early before the timer reaches 00:00.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }

            // Open Full Lock Shield / Guardian Keypad
            Button(
                onClick = onOpenFullShield,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = if (mode == LockMode.FULL_LOCKDOWN) Color.White else Color(0xFF04261C)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("open_active_shield_button")
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Open Shield")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (mode == LockMode.FULL_LOCKDOWN) {
                        "Enter Full Phone Lockdown Screen / Unlock"
                    } else {
                        "Open Guardian Passcode Unlock / Shield"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            // Interactive Quick Test: Simulate opening YouTube, Instagram, or Android Settings
            Text(
                text = "TEST LIVE INTERCEPTION SHIELD",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onSimulateBlockedApp(
                            "YouTube",
                            "com.google.android.youtube",
                            "Blocked by Aegis Social Media Shield"
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_youtube_button")
                ) {
                    Text("Test YouTube", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
                OutlinedButton(
                    onClick = {
                        onSimulateBlockedApp(
                            "Instagram",
                            "com.instagram.android",
                            "Blocked by Aegis Social Media Shield"
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_instagram_button")
                ) {
                    Text("Test Instagram", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }
    }
}

fun formatMinutesHumanReadable(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h 00m"
        else -> "${mins}m"
    }
}
