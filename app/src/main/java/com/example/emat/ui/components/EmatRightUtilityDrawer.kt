package com.example.emat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emat.domain.model.EmatNavDestination
import com.example.emat.domain.model.Timeframe
import com.example.emat.ui.theme.EmatBackground
import com.example.emat.ui.theme.EmatBorderHairline
import com.example.emat.ui.theme.EmatBorderSubtle
import com.example.emat.ui.theme.EmatFontFamily
import com.example.emat.ui.theme.EmatLongGreen
import com.example.emat.ui.theme.EmatShortRed
import com.example.emat.ui.theme.EmatSurfaceCard
import com.example.emat.ui.theme.EmatSurfaceElevated
import com.example.emat.ui.theme.EmatTechCyan
import com.example.emat.ui.theme.EmatTextMuted
import com.example.emat.ui.theme.EmatTextPrimary
import com.example.emat.ui.theme.EmatTextSubtle
import com.example.emat.ui.theme.EmatTextWhite
import com.example.emat.ui.theme.EmatWarningAmber

@Composable
fun EmatRightUtilityDrawer(
    currentDestination: EmatNavDestination,
    selectedTimeframe: Timeframe,
    selectedAssetSymbol: String,
    isScanning: Boolean,
    isEmergencyStopped: Boolean,
    onClose: () -> Unit,
    onTriggerScan: () -> Unit,
    onSelectTimeframe: (Timeframe) -> Unit,
    onAskAi: (String) -> Unit,
    onNavigate: (EmatNavDestination) -> Unit,
    onEmergencyStopToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .testTag("right_utility_drawer"),
        color = EmatBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .padding(vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONTEXT ACTIONS",
                        color = EmatTechCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontFamily = EmatFontFamily
                    )
                    Text(
                        text = "WHAT CAN I DO HERE?",
                        color = EmatTextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Utility Drawer",
                        tint = EmatTextSubtle
                    )
                }
            }

            // Current Context Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(EmatSurfaceCard)
                    .border(1.dp, EmatBorderHairline, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active: ${currentDestination.displayName.uppercase()}",
                        color = EmatTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(EmatTechCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "FOCUSED",
                            color = EmatTechCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Decision Support (Always accessible across every view!)
            UtilityActionCard(
                icon = Icons.Default.AutoAwesome,
                title = "Ask EMAT AI",
                subtitle = "Generate structured FACT / CALC / ANALYSIS / REC",
                badgeText = "INTELLIGENCE",
                badgeColor = EmatTechCyan,
                onClick = { onAskAi(currentDestination.displayName) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color = EmatBorderHairline,
                thickness = 1.dp
            )

            // Context-Specific Actions based strictly on current destination!
            when (currentDestination) {
                EmatNavDestination.DASHBOARD -> {
                    UtilitySectionHeader(title = "DASHBOARD EXECUTION")

                    UtilityActionCard(
                        icon = Icons.Default.Radar,
                        title = if (isScanning) "Scanning Universe..." else "Scan Markets Universe",
                        subtitle = "Run multi-factor institutional ECE score filter",
                        badgeText = if (isScanning) "RUNNING" else "CORE",
                        badgeColor = if (isScanning) EmatWarningAmber else EmatLongGreen,
                        onClick = onTriggerScan
                    )

                    UtilityActionCard(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        title = "View Opportunities",
                        subtitle = "Inspect ranked market setups exceeding ECE 70",
                        onClick = { onNavigate(EmatNavDestination.OPPORTUNITIES) }
                    )

                    UtilityActionCard(
                        icon = Icons.Default.Shield,
                        title = "Audit Risk Profile",
                        subtitle = "Review 1-Trade Rule & drawdown telemetry",
                        onClick = { onNavigate(EmatNavDestination.RISK) }
                    )

                    UtilityActionCard(
                        icon = Icons.Default.FolderShared,
                        title = "Inspect Evidence Records",
                        subtitle = "Verifiable observations & mathematical proofs",
                        onClick = { onNavigate(EmatNavDestination.EVIDENCE) }
                    )
                }

                EmatNavDestination.MARKET_INTELLIGENCE, EmatNavDestination.WATCHLIST -> {
                    UtilitySectionHeader(title = "CHART & TIMEFRAME CONTROLS")

                    // Timeframe Chips Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Select Timeframe:",
                            color = EmatTextSubtle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(Timeframe.MN, Timeframe.W1, Timeframe.D1, Timeframe.H4).forEach { tf ->
                                TimeframeChip(
                                    tf = tf,
                                    isSelected = selectedTimeframe == tf,
                                    onClick = { onSelectTimeframe(tf) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(Timeframe.H1, Timeframe.M30, Timeframe.M15, Timeframe.M5).forEach { tf ->
                                TimeframeChip(
                                    tf = tf,
                                    isSelected = selectedTimeframe == tf,
                                    onClick = { onSelectTimeframe(tf) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    UtilityActionCard(
                        icon = Icons.Default.Refresh,
                        title = "Refresh Market Telemetry",
                        subtitle = "Fetch latest normalized price ticks (<15s freshness)",
                        onClick = onTriggerScan
                    )

                    UtilityActionCard(
                        icon = Icons.Default.QueryStats,
                        title = "Multi-Timeframe Structure",
                        subtitle = "Inspect HTF Trend vs LTF confirmation alignment",
                        onClick = { onAskAi("Multi-Timeframe Alignment for $selectedAssetSymbol") }
                    )

                    UtilityActionCard(
                        icon = Icons.Default.FolderShared,
                        title = "Asset Evidence Trail",
                        subtitle = "Historical structural levels & ECE metrics",
                        onClick = { onNavigate(EmatNavDestination.EVIDENCE) }
                    )
                }

                EmatNavDestination.OPPORTUNITIES -> {
                    UtilitySectionHeader(title = "OPPORTUNITY ACTIONS")

                    UtilityActionCard(
                        icon = Icons.Default.Radar,
                        title = "Re-Run Universe Scanner",
                        subtitle = "Recompute ECE multi-factor scores now",
                        badgeText = "SCAN",
                        badgeColor = EmatTechCyan,
                        onClick = onTriggerScan
                    )

                    UtilityActionCard(
                        icon = Icons.Default.Shield,
                        title = "Verify Risk Feasibility",
                        subtitle = "Check 1-Trade Rule & daily loss budget",
                        onClick = { onNavigate(EmatNavDestination.RISK) }
                    )

                    UtilityActionCard(
                        icon = Icons.Default.FolderShared,
                        title = "View Opportunity Evidence",
                        subtitle = "Mathematical justification & audit log",
                        onClick = { onNavigate(EmatNavDestination.EVIDENCE) }
                    )
                }

                EmatNavDestination.TRADES -> {
                    UtilitySectionHeader(title = "EXECUTION & RISK ACTIONS")

                    UtilityActionCard(
                        icon = Icons.Default.Shield,
                        title = "One-Trade Rule Check",
                        subtitle = "Verify maximum 1 simultaneous active trade invariant",
                        badgeText = "INVARIANT",
                        badgeColor = EmatLongGreen,
                        onClick = { onAskAi("One-Trade Rule Invariant Check") }
                    )

                    UtilityActionCard(
                        icon = Icons.Default.Warning,
                        title = if (isEmergencyStopped) "Reset Emergency Stop" else "EMERGENCY STOP (HALT)",
                        subtitle = if (isEmergencyStopped) "Clear halt condition and re-arm" else "Instantly liquidate position & halt system",
                        badgeText = if (isEmergencyStopped) "HALTED" else "ARMED",
                        badgeColor = if (isEmergencyStopped) EmatShortRed else EmatWarningAmber,
                        onClick = onEmergencyStopToggle
                    )

                    UtilityActionCard(
                        icon = Icons.Default.Description,
                        title = "Post-Trade Review",
                        subtitle = "Document rationale and execution slippage",
                        onClick = { onNavigate(EmatNavDestination.JOURNAL) }
                    )
                }

                EmatNavDestination.PERFORMANCE -> {
                    UtilitySectionHeader(title = "PERFORMANCE ACTIONS")

                    UtilityActionCard(
                        icon = Icons.Default.QueryStats,
                        title = "Audit Statistical Significance",
                        subtitle = "Check sample size (>30 trades requirement)",
                        onClick = { onAskAi("Performance Sample Size Audit") }
                    )

                    UtilityActionCard(
                        icon = Icons.Default.FileDownload,
                        title = "Export Compliance Report",
                        subtitle = "Generate immutable institutional audit report",
                        badgeText = "PDF/CSV",
                        badgeColor = EmatTechCyan,
                        onClick = { onNavigate(EmatNavDestination.REPORTS) }
                    )

                    UtilityActionCard(
                        icon = Icons.Default.History,
                        title = "View Evidence Records",
                        subtitle = "Verify trade entries against market ticks",
                        onClick = { onNavigate(EmatNavDestination.EVIDENCE) }
                    )
                }

                else -> {
                    UtilitySectionHeader(title = "QUICK ACTIONS")

                    UtilityActionCard(
                        icon = Icons.Default.Radar,
                        title = "Run Market Scan",
                        subtitle = "Audit universe against strategy models",
                        onClick = onTriggerScan
                    )

                    UtilityActionCard(
                        icon = Icons.Default.FolderShared,
                        title = "View Evidence Records",
                        subtitle = "Review auditable platform actions",
                        onClick = { onNavigate(EmatNavDestination.EVIDENCE) }
                    )

                    UtilityActionCard(
                        icon = Icons.Default.Shield,
                        title = "System Risk State",
                        subtitle = "Confirm fail-safes and circuit breakers",
                        onClick = { onNavigate(EmatNavDestination.RISK) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun UtilitySectionHeader(title: String) {
    Text(
        text = title,
        color = EmatTextMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun UtilityActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeColor: Color = EmatTechCyan,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(EmatSurfaceCard)
            .border(1.dp, EmatBorderHairline, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(EmatSurfaceElevated)
                    .border(1.dp, EmatBorderHairline, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        color = EmatTextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (badgeText != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    color = EmatTextSubtle,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun TimeframeChip(
    tf: Timeframe,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) EmatTechCyan else EmatSurfaceCard)
            .border(1.dp, if (isSelected) EmatTechCyan else EmatBorderHairline, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tf.label,
            color = if (isSelected) Color.Black else EmatTextPrimary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
