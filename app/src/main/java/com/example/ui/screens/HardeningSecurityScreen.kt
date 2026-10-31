package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.service.SystemSecurityHelper
import com.example.ui.AegisUiState
import com.example.ui.theme.AmberGuardian
import com.example.ui.theme.CrimsonLock
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated

@Composable
fun HardeningSecurityScreen(
    uiState: AegisUiState,
    onRefreshPermissions: () -> Unit,
    onSimulateTamperAttempt: (String, String, String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldPrimary.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Hardening",
                            tint = EmeraldPrimary
                        )
                        Text(
                            text = "Anti-Bypass & System Control",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onRefreshPermissions,
                        modifier = Modifier.testTag("refresh_permissions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Status",
                            tint = CyanPulse
                        )
                    }
                }
                Text(
                    text = "Grant Accessibility and Device Admin below so Aegis Lock can take full control of blocked apps and prevent uninstallation during an active Guardian Timer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Accessibility Service Card
        PermissionHardeningCard(
            icon = Icons.Default.AccessibilityNew,
            title = "Accessibility App Interceptor",
            subtitle = "Detects when YouTube, Instagram, Facebook, or Android Settings opens in the foreground and immediately deploys the Lock Shield.",
            isGranted = uiState.isAccessibilityEnabled,
            grantedBadge = "INTERCEPTOR ACTIVE",
            missingBadge = "ACTION REQUIRED",
            buttonLabel = if (uiState.isAccessibilityEnabled) "Manage Accessibility Service" else "Enable Accessibility Control",
            buttonTestTag = "enable_accessibility_button",
            onActionClick = {
                SystemSecurityHelper.openAccessibilitySettings(context)
            }
        )

        // 2. Device Admin Anti-Uninstall Card
        PermissionHardeningCard(
            icon = Icons.Default.AdminPanelSettings,
            title = "Device Admin Anti-Uninstall Guard",
            subtitle = "Blocks the Android Package Uninstaller from removing Aegis Lock while your focus timer or Guardian Passcode lock is active.",
            isGranted = uiState.isDeviceAdminEnabled,
            grantedBadge = "UNINSTALL LOCKED",
            missingBadge = "RECOMMENDED",
            buttonLabel = if (uiState.isDeviceAdminEnabled) "Device Admin Protected" else "Activate Anti-Uninstall Guard",
            buttonTestTag = "enable_device_admin_button",
            onActionClick = {
                SystemSecurityHelper.requestDeviceAdminActivation(context)
            }
        )

        // 3. Interactive Anti-Tamper Test Suite
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmberGuardian.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhonelinkSetup,
                        contentDescription = "Test Suite",
                        tint = AmberGuardian
                    )
                    Column {
                        Text(
                            text = "Test Anti-Bypass Shield Instantly",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Verify how Aegis Lock intercepts Settings tampering, Uninstall attempts, or Social Media.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onSimulateTamperAttempt(
                                "System Settings / Uninstall Guard",
                                "com.android.settings",
                                "Anti-Bypass Guard: Cannot open Android Settings or Uninstall while Guardian Timer is active."
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_settings_tamper_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Tamper Guard", tint = CrimsonLock, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Settings Guard", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            onSimulateTamperAttempt(
                                "Facebook",
                                "com.facebook.katana",
                                "Blocked by Partial Distraction Shield"
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_facebook_block_button")
                    ) {
                        Text("Test Facebook Block", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 4. 100% Offline Zero-Data-Transfer Audit Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyanPulse.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .testTag("offline_privacy_audit_card")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CyanPulse.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Zero Network",
                            tint = CyanPulse
                        )
                    }
                    Column {
                        Text(
                            text = "Zero-Network & Least-Privilege Audit",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "100% air-gapped local storage. Zero bytes ever leave your phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanPulse
                        )
                    }
                }

                val auditRows = listOf(
                    "Internet / Network Permission" to "REMOVED (0 network sockets)",
                    "Camera & Microphone Permission" to "REMOVED (Not requested)",
                    "Location & Contacts Permission" to "REMOVED (Not requested)",
                    "Passcode & Session Database" to "100% On-Device Room + SHA-256"
                )
                for ((label, status) in auditRows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianSurfaceElevated.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GppGood,
                                contentDescription = "Verified",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionHardeningCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    grantedBadge: String,
    missingBadge: String,
    buttonLabel: String,
    buttonTestTag: String,
    onActionClick: () -> Unit
) {
    val accent = if (isGranted) EmeraldPrimary else AmberGuardian

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ObsidianSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = title, tint = accent)
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isGranted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Enabled",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = if (isGranted) grantedBadge else missingBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) ObsidianSurfaceElevated else accent,
                    contentColor = if (isGranted) Color.White else Color(0xFF1A1102)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(buttonTestTag)
            ) {
                Text(
                    text = buttonLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
