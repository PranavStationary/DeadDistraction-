package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.LockMode
import com.example.ui.AegisUiState
import com.example.ui.theme.AmberGuardian
import com.example.ui.theme.CrimsonLock
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultAnalyticsScreen(
    uiState: AegisUiState
) {
    val totalMinutesLocked = uiState.sessionHistory.sumOf { it.durationMinutes }
    val totalBlockedAttempts = uiState.appRules.sumOf { it.blockedAttemptsCount } +
        uiState.interceptionLogs.size.coerceAtLeast(0)
    val totalSessions = uiState.sessionHistory.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Summary Metrics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    value = formatMinutesHumanReadable(totalMinutesLocked),
                    label = "LOCKED FOCUS",
                    accent = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    value = totalBlockedAttempts.toString(),
                    label = "INTERCEPTED",
                    accent = CrimsonLock,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    value = totalSessions.toString(),
                    label = "SESSIONS",
                    accent = AmberGuardian,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Interception Audit Log Section
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Blocked Attempts",
                    tint = CrimsonLock,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Live Distraction Interception Log",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (uiState.interceptionLogs.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ObsidianSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(22.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Clean Log",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = "No Distraction Attempts Recorded Yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Text(
                            text = "Whenever YouTube, Instagram, Facebook, or Android Settings is blocked during an active lock, the exact timestamp and reason appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(uiState.interceptionLogs, key = { it.id }) { log ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CrimsonLock.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                        .testTag("interception_log_item_${log.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CrimsonLock.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Blocked",
                                tint = CrimsonLock,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.appName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatTimestamp(log.timestampEpochMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = log.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = CrimsonLock
                            )
                        }
                    }
                }
            }
        }

        // 3. Past Lock Sessions History
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Session History",
                    tint = CyanPulse,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Guardian Lock Session History",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (uiState.sessionHistory.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ObsidianSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockClock,
                            contentDescription = "No Sessions",
                            tint = CyanPulse,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Start your first Partial Shield or Full Phone Lockdown on the Fortress tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(uiState.sessionHistory, key = { it.id }) { session ->
                val mode = runCatching { LockMode.valueOf(session.mode) }.getOrDefault(LockMode.PARTIAL_SHIELD)
                val accent = if (mode == LockMode.FULL_LOCKDOWN) CrimsonLock else EmeraldPrimary
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Guardian: ${session.guardianRecipientLabel} • Duration: ${formatMinutesHumanReadable(session.durationMinutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when {
                                    session.isActive -> "Status: Currently Active"
                                    session.unlockedWithPasscode -> "Status: Unlocked via Guardian Passcode"
                                    session.completedNaturally -> "Status: Full Timer Completed"
                                    else -> "Status: Ended"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianSurfaceElevated
                        ) {
                            Text(
                                text = "${session.interceptionsCount} blocks",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricStatCard(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface,
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatTimestamp(epochMs: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
    return sdf.format(Date(epochMs))
}
