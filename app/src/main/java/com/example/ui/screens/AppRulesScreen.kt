package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppRuleEntity
import com.example.ui.AegisUiState
import com.example.ui.theme.AmberGuardian
import com.example.ui.theme.CrimsonLock
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated

@Composable
fun AppRulesScreen(
    uiState: AegisUiState,
    onTogglePartialBlock: (AppRuleEntity, Boolean) -> Unit,
    onToggleFullWhitelist: (AppRuleEntity, Boolean) -> Unit,
    onSimulateInterception: (String, String, String) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = Partial Blocklist, 1 = Full Lockdown Whitelist
    var searchQuery by remember { mutableStateOf("") }

    val isSessionActive = uiState.activeSession?.isActive == true

    val filteredApps = remember(uiState.appRules, searchQuery, selectedTabIndex) {
        uiState.appRules.filter { rule ->
            val matchesQuery = searchQuery.isBlank() ||
                rule.appName.contains(searchQuery, ignoreCase = true) ||
                rule.packageName.contains(searchQuery, ignoreCase = true)
            matchesQuery
        }.sortedWith(
            if (selectedTabIndex == 0) {
                compareByDescending<AppRuleEntity> { it.isBlockedInPartial }.thenBy { it.appName }
            } else {
                compareByDescending<AppRuleEntity> { it.isAllowedInFullLockdown }.thenBy { it.appName }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sub-Tab Switcher: Partial Social Media Shield vs Full Lockdown Whitelist
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ObsidianSurfaceElevated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf(
                    "Social & App Blocklist" to EmeraldPrimary,
                    "Full Lockdown Whitelist" to CrimsonLock
                )
                tabs.forEachIndexed { idx, (title, accent) ->
                    val selected = selectedTabIndex == idx
                    val bg by animateColorAsState(
                        targetValue = if (selected) accent else Color.Transparent,
                        label = "rules_tab_$idx"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(bg)
                            .clickable { selectedTabIndex = idx }
                            .padding(vertical = 10.dp, horizontal = 8.dp)
                            .testTag("rules_subtab_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) {
                                if (idx == 0) Color(0xFF04261C) else Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Info Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ObsidianSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (selectedTabIndex == 0) Icons.Default.Block else Icons.Default.CheckCircle,
                    contentDescription = "Rule Policy",
                    tint = if (selectedTabIndex == 0) EmeraldPrimary else CyanPulse
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedTabIndex == 0) {
                            "Partial Distraction Shield Blocklist"
                        } else {
                            "Full Mobile Lockdown Allowed Apps"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Text(
                        text = if (selectedTabIndex == 0) {
                            "Toggled apps (YouTube, Instagram, Facebook, TikTok, etc.) are completely blocked while Partial Shield is active."
                        } else {
                            "During Full Mobile Lockdown, EVERY app on your phone is blocked EXCEPT the essential utilities enabled below (Phone, SMS)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search YouTube, Instagram, Facebook, Phone...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Apps") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_rules_search_input")
        )

        if (isSessionActive) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AmberGuardian.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Rules Locked",
                        tint = AmberGuardian,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Strict Lock Active: You can add more restrictions, but cannot unblock apps until timer or Guardian Passcode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberGuardian
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { rule ->
                AppRuleItemCard(
                    rule = rule,
                    isPartialModeTab = selectedTabIndex == 0,
                    onToggle = { checked ->
                        if (selectedTabIndex == 0) {
                            onTogglePartialBlock(rule, checked)
                        } else {
                            onToggleFullWhitelist(rule, checked)
                        }
                    },
                    onTestInterception = {
                        onSimulateInterception(
                            rule.appName,
                            rule.packageName,
                            if (selectedTabIndex == 0) {
                                "Intercepted by Partial Social Media Shield"
                            } else {
                                "Intercepted by Full Mobile Lockdown"
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AppRuleItemCard(
    rule: AppRuleEntity,
    isPartialModeTab: Boolean,
    onToggle: (Boolean) -> Unit,
    onTestInterception: () -> Unit
) {
    val isChecked = if (isPartialModeTab) rule.isBlockedInPartial else rule.isAllowedInFullLockdown
    val brandColor = Color(rule.brandHexColor)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isChecked) {
                    if (isPartialModeTab) CrimsonLock.copy(alpha = 0.4f) else EmeraldPrimary.copy(alpha = 0.4f)
                } else {
                    Color.White.copy(alpha = 0.06f)
                },
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("app_rule_card_${rule.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App Monogram Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(brandColor.copy(alpha = 0.2f))
                    .border(1.dp, brandColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rule.appName.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = brandColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = rule.appName,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (rule.blockedAttemptsCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = CrimsonLock.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "${rule.blockedAttemptsCount} blocked",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonLock,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = rule.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isPartialModeTab) {
                            if (isChecked) "BLOCKED IN PARTIAL SHIELD" else "Allowed"
                        } else {
                            if (isChecked) "WHITELISTED IN FULL LOCK" else "Blocked in Full Lock"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPartialModeTab) {
                            if (isChecked) CrimsonLock else EmeraldPrimary
                        } else {
                            if (isChecked) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    if ((isPartialModeTab && isChecked) || (!isPartialModeTab && !isChecked)) {
                        TextButton(
                            onClick = onTestInterception,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("test_intercept_${rule.packageName}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Simulate Block",
                                modifier = Modifier.size(14.dp),
                                tint = AmberGuardian
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Preview Shield",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberGuardian
                            )
                        }
                    }
                }
            }

            Switch(
                checked = isChecked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (isPartialModeTab) CrimsonLock else EmeraldPrimary,
                    checkedTrackColor = if (isPartialModeTab) CrimsonLock.copy(alpha = 0.3f) else EmeraldPrimary.copy(alpha = 0.3f)
                ),
                modifier = Modifier.testTag("app_rule_switch_${rule.packageName}")
            )
        }
    }
}
