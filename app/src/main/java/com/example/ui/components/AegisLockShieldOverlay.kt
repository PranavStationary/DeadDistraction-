package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LockMode
import com.example.ui.theme.AmberGuardian
import com.example.ui.theme.CrimsonLock
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AegisLockShieldOverlay(
    mode: LockMode,
    remainingSeconds: Long,
    progressFraction: Float,
    guardianLabel: String,
    isPasscodeSealed: Boolean,
    interceptedAppName: String,
    interceptedReason: String,
    onVerifyPasscode: suspend (String) -> Boolean,
    onOpenDialer: () -> Unit,
    onOpenMessaging: () -> Unit,
    onReturnToSafeArea: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var enteredDigits by remember { mutableStateOf("") }
    var showKeypad by remember { mutableStateOf(false) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val shakeOffset = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "shield_aura")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val primaryAccent = if (mode == LockMode.FULL_LOCKDOWN) CrimsonLock else EmeraldPrimary
    val secondaryAccent = if (mode == LockMode.FULL_LOCKDOWN) AmberGuardian else CyanPulse

    fun handleDigitInput(digit: String) {
        if (enteredDigits.length >= 6) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        errorMessage = null
        val updated = enteredDigits + digit
        enteredDigits = updated

        if (updated.length == 6) {
            scope.launch {
                val success = onVerifyPasscode(updated)
                if (!success) {
                    failedAttempts += 1
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    errorMessage = "Invalid Guardian Passcode (Attempt #$failedAttempts)"
                    // iOS-style damped horizontal shake animation
                    val keyframes = listOf(-26f, 26f, -18f, 18f, -10f, 10f, 0f)
                    for (target in keyframes) {
                        shakeOffset.animateTo(
                            targetValue = target,
                            animationSpec = tween(durationMillis = 48, easing = LinearEasing)
                        )
                    }
                    enteredDigits = ""
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF04070D),
                        ObsidianBackground,
                        Color(0xFF091322)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("lock_shield_overlay"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // iOS Dynamic Island Interception Pill
            Surface(
                shape = CircleShape,
                color = ObsidianSurfaceElevated.copy(alpha = 0.92f),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(1.dp, primaryAccent.copy(alpha = 0.45f), CircleShape)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (mode == LockMode.FULL_LOCKDOWN) Icons.Default.GppBad else Icons.Default.Shield,
                        contentDescription = "Active Shield",
                        tint = primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (mode == LockMode.FULL_LOCKDOWN) "FULL PHONE LOCKDOWN" else "DISTRACTION INTERCEPTED",
                        style = MaterialTheme.typography.labelMedium,
                        color = primaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Intercepted App Banner
            Text(
                text = interceptedAppName,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = interceptedReason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Breathing Concentric Countdown Ring
            Box(
                modifier = Modifier.size(230.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(pulseScale)
                ) {
                    val strokeWidth = 14.dp.toPx()
                    val diameter = size.minDimension - strokeWidth * 2
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

                    // Outer soft aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryAccent.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension / 2f
                    )

                    // Background track arc
                    drawArc(
                        color = Color.White.copy(alpha = 0.08f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Active countdown sweep arc
                    val sweep = (1f - progressFraction).coerceIn(0.03f, 1f) * 360f
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(primaryAccent, secondaryAccent, primaryAccent)
                        ),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = primaryAccent,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatDurationHhMmSs(remainingSeconds),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 32.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.testTag("shield_countdown_timer")
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "REMAINING LOCK",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // In Full Mobile Lockdown: Essential Phone & SMS Quick Dock
            if (mode == LockMode.FULL_LOCKDOWN) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ObsidianSurface.copy(alpha = 0.92f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(22.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "WHITELISTED ESSENTIAL UTILITIES",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Only Phone Calls and SMS Messaging are permitted during Full Mobile Lockdown.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onOpenDialer,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary.copy(alpha = 0.18f),
                                    contentColor = EmeraldPrimary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("emergency_dialer_button")
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Phone Calls", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Phone Calls", style = MaterialTheme.typography.labelLarge)
                            }
                            Button(
                                onClick = onOpenMessaging,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanPulse.copy(alpha = 0.18f),
                                    contentColor = CyanPulse
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("emergency_sms_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "SMS Messages", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Messages", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Toggle between Guardian Passcode Keypad and Status Card
            AnimatedVisibility(
                visible = !showKeypad,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ObsidianSurfaceElevated.copy(alpha = 0.75f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AmberGuardian.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AmberGuardian.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Guardian Key",
                                    tint = AmberGuardian
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPasscodeSealed) "Passcode Sealed with $guardianLabel" else "Protected by Guardian Passcode",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Text(
                                    text = "No bypass, uninstall, or force-stop is possible. Only waiting for the timer or entering $guardianLabel's 6-digit passcode can unlock early.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showKeypad = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberGuardian,
                            contentColor = Color(0xFF1E1002)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("open_guardian_keypad_button")
                    ) {
                        Icon(Icons.Default.Key, contentDescription = "Enter Passcode")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enter Guardian Passcode to Unlock",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onReturnToSafeArea,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("return_safe_area_button")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Return Safe", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (mode == LockMode.PARTIAL_SHIELD) "Close Blocked App & Focus" else "Return to Aegis Safe Hub",
                            color = Color.White
                        )
                    }
                }
            }

            // iOS-style Tactile 6-Digit Passcode Keypad
            AnimatedVisibility(
                visible = showKeypad,
                enter = fadeIn() + scaleIn(initialScale = 0.92f),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enter $guardianLabel's 6-Digit Passcode",
                        style = MaterialTheme.typography.titleSmall,
                        color = AmberGuardian
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 6 Passcode Dots with horizontal shake offset on error
                    Row(
                        modifier = Modifier
                            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (i in 0 until 6) {
                            val isFilled = i < enteredDigits.length
                            val dotScale by animateFloatAsState(
                                targetValue = if (isFilled) 1.18f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "dot_scale_$i"
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .scale(dotScale)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) AmberGuardian else Color.Transparent
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (errorMessage != null) CrimsonLock else AmberGuardian,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonLock,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("passcode_error_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9")
                    )
                    for (row in rows) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(22.dp),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            for (digit in row) {
                                IosKeypadButton(
                                    digit = digit,
                                    onClick = { handleDigitInput(digit) }
                                )
                            }
                        }
                    }

                    // Bottom row: Cancel | 0 | Backspace
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .clickable {
                                    enteredDigits = ""
                                    errorMessage = null
                                    showKeypad = false
                                }
                                .testTag("keypad_cancel_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IosKeypadButton(
                            digit = "0",
                            onClick = { handleDigitInput("0") }
                        )

                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (enteredDigits.isNotEmpty()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        enteredDigits = enteredDigits.dropLast(1)
                                    }
                                }
                                .testTag("keypad_backspace_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IosKeypadButton(
    digit: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "keypad_btn_$digit"
    )

    Box(
        modifier = Modifier
            .size(68.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isPressed) Color.White.copy(alpha = 0.28f)
                else ObsidianSurfaceElevated.copy(alpha = 0.85f)
            )
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("keypad_digit_$digit"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )
    }
}

fun formatDurationHhMmSs(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
