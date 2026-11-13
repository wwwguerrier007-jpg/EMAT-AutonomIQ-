package com.example.emat.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emat.ui.theme.EmatFontFamily
import com.example.emat.data.local.entity.SystemHealthEntity
import com.example.emat.domain.model.NewsEvent
import com.example.emat.ui.components.StatusBadge
import com.example.emat.ui.theme.EmatBackground
import com.example.emat.ui.theme.EmatBorderHairline
import com.example.emat.ui.theme.EmatBorderSubtle
import com.example.emat.ui.theme.EmatLongGreen
import com.example.emat.ui.theme.EmatShortRed
import com.example.emat.ui.theme.EmatSurfaceActive
import com.example.emat.ui.theme.EmatSurfaceCard
import com.example.emat.ui.theme.EmatSurfaceElevated
import com.example.emat.ui.theme.EmatTechCyan
import com.example.emat.ui.theme.EmatTextMuted
import com.example.emat.ui.theme.EmatTextOffset
import com.example.emat.ui.theme.EmatTextPrimary
import com.example.emat.ui.theme.EmatTextSubtle
import com.example.emat.ui.theme.EmatTextWhite
import com.example.emat.ui.theme.EmatWarningAmber

@Composable
fun IntelligenceScreen(
    sastTime: String,
    highImpactNews: List<NewsEvent>,
    systemHealth: List<SystemHealthEntity>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(EmatBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("intelligence_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Intelligence Hub Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, EmatBorderHairline, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ARCHITECTURE & VERIFIABLE INTELLIGENCE",
                        color = EmatTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "EMAT Institutional Framework",
                        color = EmatTextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Complete transparency into algorithmic scoring, multi-timeframe alignment, pre-news buffers, and broker gateway state.",
                        color = EmatTextSubtle,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // ECE Scoring Model Architecture Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, EmatBorderHairline, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ECE SCORE WEIGHTING (0–100)",
                            color = EmatTextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        StatusBadge(status = "QUALIFIED >= 70")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val weights = listOf(
                        Triple("Trend Direction & Alignment", "20%", "H4/H1 trend vector agreement"),
                        Triple("Market Structure & Displacement", "20%", "Key liquidity break & fair value"),
                        Triple("Momentum & Volatility Expansion", "15%", "Relative range acceleration"),
                        Triple("Setup Confirmation & Trigger", "15%", "Candle reaction at zone"),
                        Triple("Timeframe Alignment", "15%", "H4 / H1 / M15 concordance"),
                        Triple("Risk / Spread Penalty", "10%", "Friction & stop distance cost"),
                        Triple("News Risk Adjustment", "5%", "Upcoming scheduled macro events")
                    )

                    weights.forEach { (title, weight, desc) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = EmatSurfaceActive
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, color = EmatTextWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text(desc, color = EmatTextMuted, fontSize = 9.sp)
                                }
                                Text(weight, color = EmatTechCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // Multi-Timeframe Architecture
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, EmatBorderHairline, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MULTI-TIMEFRAME HIERARCHY",
                        color = EmatTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val timeframes = listOf(
                        Pair("MN / W1", "Macro Regime & Major Institutional Levels"),
                        Pair("D1 / H4", "Structural Bias & Trend Definition"),
                        Pair("H1 / M30", "Intermediate Structure & Setup Location"),
                        Pair("M15 / M5", "Precise Execution Trigger & Invalidation Anchor")
                    )

                    timeframes.forEach { (tf, role) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(tf, color = EmatTechCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = EmatFontFamily)
                            Text(role, color = EmatTextSubtle, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // News Intelligence Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, EmatBorderHairline, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NEWS INTELLIGENCE ARCHITECTURE",
                                color = EmatTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "SAST: $sastTime",
                                color = EmatTextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        StatusBadge(status = "NOT CONNECTED")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "High-Impact Economic Calendar (South African Standard Time):",
                        color = EmatTextSubtle,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    highImpactNews.forEach { event ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = EmatSurfaceActive
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(event.title, color = EmatTextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Impact: ${event.impact} | Curr: ${event.currency} | ${event.affectedInstruments.joinToString()}",
                                        color = EmatTextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(event.scheduledSast, color = EmatWarningAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = EmatFontFamily)
                                    Text("In ${event.timeRemainingMinutes}m", color = EmatTextMuted, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // MetaTrader 5 Gateway Adapter Status
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, EmatBorderHairline, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "METATRADER 5 GATEWAY ADAPTER",
                            color = EmatTextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        StatusBadge(status = "NOT CONNECTED")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Institutional Execution Gateway for MT5 is configured in fail-safe disconnected posture. Zero trades will be dispatched until gateway socket authentication is established.",
                        color = EmatTextSubtle,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Detailed System Health Diagnostic Matrix
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, EmatBorderHairline, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SYSTEM HEALTH DIAGNOSTICS",
                        color = EmatTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    systemHealth.forEach { component ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = EmatSurfaceActive
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(component.componentKey, color = EmatTextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = EmatFontFamily)
                                    Text(component.details, color = EmatTextMuted, fontSize = 9.sp)
                                }
                                StatusBadge(status = component.status)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

