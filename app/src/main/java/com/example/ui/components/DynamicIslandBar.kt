package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LockMode
import com.example.data.LockSessionEntity
import com.example.ui.theme.AmberGuardian
import com.example.ui.theme.CrimsonLock
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianSurfaceElevated

@Composable
fun DynamicIslandBar(
    activeSession: LockSessionEntity?,
    currentTimeMs: Long,
    onExpandActiveShield: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLocked = activeSession?.isActive == true
    val mode = activeSession?.let {
        runCatching { LockMode.valueOf(it.mode) }.getOrDefault(LockMode.PARTIAL_SHIELD)
    } ?: LockMode.PARTIAL_SHIELD

    val borderColor by animateColorAsState(
        targetValue = when {
            !isLocked -> EmeraldPrimary.copy(alpha = 0.35f)
            mode == LockMode.FULL_LOCKDOWN -> CrimsonLock.copy(alpha = 0.7f)
            else -> AmberGuardian.copy(alpha = 0.7f)
        },
        label = "island_border"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF05080F),
            tonalElevation = 12.dp,
            shadowElevation = 10.dp,
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                .border(1.2.dp, borderColor, RoundedCornerShape(28.dp))
                .clickable(enabled = isLocked) { onExpandActiveShield() }
                .testTag("dynamic_island_pill")
        ) {
            AnimatedContent(
                targetState = isLocked,
                label = "dynamic_island_content"
            ) { locked ->
                if (locked && activeSession != null) {
                    val remainingSec = activeSession.remainingSeconds(currentTimeMs)
                    val accent = if (mode == LockMode.FULL_LOCKDOWN) CrimsonLock else EmeraldPrimary
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Active Lock",
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = if (mode == LockMode.FULL_LOCKDOWN) "FULL PHONE LOCKDOWN" else "PARTIAL APP SHIELD",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap to open Shield • ${if (activeSession.isPasscodeSealed) "Passcode Sealed" else "Guardian Key Active"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurfaceElevated
                        ) {
                            Text(
                                text = formatDurationHhMmSs(remainingSec),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("island_timer_text")
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Offline Vault",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "AEGIS LOCK",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.OfflineBolt,
                            contentDescription = "100% Offline",
                            tint = AmberGuardian,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "100% OFFLINE ZERO-BYPASS CORE",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary
                        )
                    }
                }
            }
        }
    }
}
