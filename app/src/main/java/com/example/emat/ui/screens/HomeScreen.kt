package com.example.emat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emat.ui.theme.EmatFontFamily
import com.example.emat.data.local.entity.InstrumentEntity
import com.example.emat.data.local.entity.SystemHealthEntity
import com.example.emat.domain.model.AccountEntitlement
import com.example.emat.domain.model.ActiveTrade
import com.example.emat.domain.model.EmatAccount
import com.example.emat.domain.model.EntitlementState
import com.example.emat.domain.model.NewsEvent
import com.example.emat.domain.model.Opportunity
import com.example.emat.domain.service.MarketDataState
import com.example.emat.ui.components.EmatRiskExposureChart
import com.example.emat.ui.components.OpportunityCard
import com.example.emat.ui.components.StatusBadge
import com.example.emat.ui.theme.EmatBackground
import com.example.emat.ui.theme.EmatBorderHairline
import com.example.emat.ui.theme.EmatBorderSubtle
import com.example.emat.ui.theme.EmatBullishGreen
import com.example.emat.ui.theme.EmatLongGreen
import com.example.emat.ui.theme.EmatShortRed
import com.example.emat.ui.theme.EmatSurfaceActive
import com.example.emat.ui.theme.EmatSurfaceCard
import com.example.emat.ui.theme.EmatSurfaceElevated
import com.example.emat.ui.theme.EmatTechCyan
import com.example.emat.ui.theme.EmatTechCyanDim
import com.example.emat.ui.theme.EmatTextMuted
import com.example.emat.ui.theme.EmatTextOffset
import com.example.emat.ui.theme.EmatTextPrimary
import com.example.emat.ui.theme.EmatTextSubtle
import com.example.emat.ui.theme.EmatTextWhite
import com.example.emat.ui.theme.EmatWarningAmber

@Composable
fun HomeScreen(
    isScanning: Boolean,
    scannerStatus: String,
    lastScanTime: String,
    isOffline: Boolean,
    emergencyStopEngaged: Boolean,
    sastTime: String,
    instruments: List<InstrumentEntity>,
    topOpportunities: List<Opportunity>,
    activeTrade: ActiveTrade?,
    systemHealth: List<SystemHealthEntity>,
    highImpactNews: List<NewsEvent>,
    onRefreshMarketData: () -> Unit = {},
    marketDataState: MarketDataState = MarketDataState.NOT_CONNECTED,
    currentAccount: EmatAccount? = null,
    currentAccountEntitlement: AccountEntitlement? = null,
    databaseIntegrityStatus: String = "INTEGRITY CHECK NOT RUN",
    databaseIntegrityPositive: Boolean? = null,
    aiIntelligenceStatus: String = "LOCAL ANALYZER NOT READY",
    aiIntelligencePositive: Boolean = false,
    onTriggerScan: () -> Unit,
    onEmergencyStopToggle: () -> Unit,
    onOpportunityClick: (Opportunity) -> Unit,
    onNavigateToScanner: () -> Unit,
    onStartTrial: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(EmatBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section: UNIEE Autonomous Ecosystem Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(1.dp, EmatBorderHairline, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_uniee_logo),
                            contentDescription = "UNIEE Logo",
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column {
                        Text(
                            text = "EMAT AUTONOMIQ",
                            color = EmatTextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Autonomous — Product of UNIEE",
                            color = EmatTechCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Section: Top Sleek Metric Grid (Scanner Status & Data Freshness)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 1: Scanner Status
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, EmatBorderHairline, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "SCANNER STATUS",
                            color = EmatTextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isScanning) "Scanning" else "Idle",
                                color = EmatTextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (emergencyStopEngaged) "Halted" else "Active",
                                color = if (emergencyStopEngaged) EmatShortRed else EmatLongGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }

                        // Mini progress line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0x0DFFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(if (isScanning) 0.85f else 0.45f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(EmatTechCyan)
                            )
                        }
                    }
                }

                // Metric 2: Data Freshness
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, EmatBorderHairline, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DATA FRESHNESS",
                                color = EmatTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            androidx.compose.material3.IconButton(
                                onClick = onRefreshMarketData,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("refresh_market_data_button")
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                                    contentDescription = "Refresh Market Data Quotes",
                                    tint = EmatTechCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        val latestOpp = topOpportunities.firstOrNull()
                        val ageText = if (latestOpp != null && marketDataState != MarketDataState.NOT_CONNECTED) {
                            "${latestOpp.freshnessSeconds}s"
                        } else {
                            "--"
                        }
                        val originLabel = when {
                            marketDataState == MarketDataState.CONNECTED && latestOpp != null && !latestOpp.isStale -> "LIVE FEED"
                            else -> "UNAVAILABLE"
                        }
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = ageText,
                                color = if (ageText == "--") EmatTextMuted else EmatTextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = originLabel,
                                color = if (originLabel == "LIVE FEED") EmatLongGreen else EmatTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = EmatFontFamily,
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }

                        Text(
                            text = marketDataState.label,
                            color = EmatTextOffset,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        // Section: Universe & One-Trade Rule Secondary Metrics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Active Universe
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, EmatBorderHairline, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("REGISTRY UNIVERSE", color = EmatTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${instruments.size} Instruments", color = EmatTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = EmatFontFamily)
                        Text("Forex, Metals, Indices, Crypto", color = EmatTextOffset, fontSize = 9.sp)
                    }
                }

                // One-Trade Rule Enforcer
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, EmatBorderHairline, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("ONE-TRADE RULE", color = EmatTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (activeTrade != null) "1 / 1 (FULL)" else "0 / 1 (OPEN)",
                            color = if (activeTrade != null) EmatWarningAmber else EmatLongGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = EmatFontFamily
                        )
                        Text("Strict mechanical limit", color = EmatTextOffset, fontSize = 9.sp)
                    }
                }
            }
        }

        // Section: Real-Time Risk Exposure Monitor
        item {
            val accountToDisplay = currentAccount ?: EmatAccount(
                id = "EMAT-INST-001",
                name = "EMAT Alpha Vault",
                role = "Lead Operator",
                tier = "Tier 1 Master",
                dailyLossLimit = "2.0%",
                maxPositionSize = "1.00 Lot",
                isLiveExecutionAllowed = false,
                permissions = emptyList()
            )
            EmatRiskExposureChart(
                activeTrade = activeTrade,
                account = accountToDisplay
            )
        }

        // Section: Entitlement & Access Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, EmatBorderHairline, RoundedCornerShape(14.dp))
                    .testTag("entitlement_status_card"),
                colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ENTITLEMENT & ACCESS",
                                color = EmatTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            val stateLabel = currentAccountEntitlement?.state?.name ?: "NOT_SUBSCRIBED"
                            Text(
                                text = stateLabel,
                                color = when (currentAccountEntitlement?.state) {
                                    EntitlementState.SUBSCRIPTION_ACTIVE, EntitlementState.TRIAL_ACTIVE -> EmatBullishGreen
                                    EntitlementState.TRIAL_EXPIRED, EntitlementState.SUBSCRIPTION_EXPIRED -> EmatShortRed
                                    else -> EmatTechCyan
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = EmatFontFamily
                            )
                        }

                        if (currentAccountEntitlement?.hasClockAnomaly == true) {
                            Text(
                                text = "CLOCK ANOMALY DETECTED",
                                color = EmatWarningAmber,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("REMAINING TIME", color = EmatTextOffset, fontSize = 9.sp)
                            val remainingText = when {
                                currentAccountEntitlement?.state == EntitlementState.TRIAL_ACTIVE -> {
                                    val days = currentAccountEntitlement.remainingTrialDays
                                    val hours = currentAccountEntitlement.remainingTrialHours
                                    if (days > 0) "$days Days ($hours h)" else "$hours Hours"
                                }
                                currentAccountEntitlement?.state == EntitlementState.SUBSCRIPTION_ACTIVE -> {
                                    val exp = currentAccountEntitlement.subscriptionExpiresAt
                                    if (exp != null) "${((exp - System.currentTimeMillis()) / (24 * 3600 * 1000L)).coerceAtLeast(0)} Days" else "UNAVAILABLE"
                                }
                                else -> "UNAVAILABLE"
                            }
                            Text(remainingText, color = EmatTextWhite, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("BILLING STATUS", color = EmatTextOffset, fontSize = 9.sp)
                            Text("NOT_CONNECTED", color = EmatTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    if (currentAccountEntitlement == null || currentAccountEntitlement.state == EntitlementState.NOT_SUBSCRIBED) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onStartTrial,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("start_trial_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmatTechCyan,
                                contentColor = EmatTextWhite
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Start 14-Day Free Trial", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Section: Full Scan Action Button
        item {
            Button(
                onClick = onTriggerScan,
                enabled = !isScanning && !emergencyStopEngaged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("run_market_scan_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmatTechCyan,
                    contentColor = EmatTextWhite,
                    disabledContainerColor = Color(0xFF1E2430),
                    disabledContentColor = EmatTextMuted
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = EmatTextWhite,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "SCANNING ${instruments.size} REGISTRY INSTRUMENTS...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "RUN FULL MARKET SCAN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Section Header: Top 3 Opportunities
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "TOP 3 OPPORTUNITIES",
                    color = EmatTextSubtle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${topOpportunities.count { it.symbol != "NONE" }} Active Setups",
                    color = EmatTechCyan,
                    fontSize = 11.sp,
                    fontFamily = EmatFontFamily
                )
            }
        }

        // Top 3 Opportunity Items
        if (topOpportunities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, EmatBorderHairline, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No scan results yet.", color = EmatTextSubtle, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Trigger the market scan to rank setups against ECE thresholds.", color = EmatTextOffset, fontSize = 11.sp)
                    }
                }
            }
        } else if (topOpportunities.all { it.symbol == "NONE" }) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, EmatBorderHairline, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("NO QUALIFIED SETUPS IDENTIFIED", color = EmatTechCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("All scanned assets failed strict institutional entry criteria. Zero trades forced.", color = EmatTextOffset, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(topOpportunities) { opp ->
                OpportunityCard(
                    opportunity = opp,
                    onClick = { onOpportunityClick(opp) }
                )
            }
        }

        // Section: Network Infrastructure (Matching Sleek Interface HTML)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, EmatBorderHairline, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NETWORK INFRASTRUCTURE",
                            color = EmatTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "v1.0.4-modular",
                            color = EmatTextOffset,
                            fontSize = 9.sp,
                            fontFamily = EmatFontFamily
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfraItem(label = "Database", status = databaseIntegrityStatus, isPositive = databaseIntegrityPositive)
                            InfraItem(label = "Broker Terminal", status = "NOT_CONNECTED", isPositive = false)
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val dataFeedStatus = when {
                                isOffline -> "OFFLINE"
                                marketDataState == MarketDataState.NOT_CONNECTED -> "NOT_CONNECTED"
                                else -> "LOCAL TEST HARNESS"
                            }
                            val isDataPositive = !isOffline && marketDataState != MarketDataState.NOT_CONNECTED
                            InfraItem(label = "Market Data Feed", status = dataFeedStatus, isPositive = isDataPositive)
                            InfraItem(label = "AI Intelligence", status = aiIntelligenceStatus, isPositive = aiIntelligencePositive)
                        }
                    }
                }
            }
        }

        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InfraItem(
    label: String,
    status: String,
    isPositive: Boolean?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val dotColor = when (isPositive) {
            true -> EmatLongGreen
            false -> EmatShortRed
            null -> EmatWarningAmber
        }

        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = 0.5f))
        )

        Text(
            text = "$label: $status",
            color = EmatTextSubtle,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

