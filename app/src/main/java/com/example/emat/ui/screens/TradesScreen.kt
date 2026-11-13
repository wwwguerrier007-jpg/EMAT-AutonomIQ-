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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emat.ui.theme.EmatFontFamily
import com.example.emat.domain.model.ActiveTrade
import com.example.emat.domain.model.TradeDirection
import com.example.emat.ui.components.StatusBadge
import com.example.emat.ui.theme.EmatBackground
import com.example.emat.ui.theme.EmatBorderHairline
import com.example.emat.ui.theme.EmatBorderSubtle
import com.example.emat.ui.theme.EmatLongGreen
import com.example.emat.ui.theme.EmatShortRed
import com.example.emat.ui.theme.EmatShortRedBorder
import com.example.emat.ui.theme.EmatShortRedDim
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
fun TradesScreen(
    activeTrade: ActiveTrade?,
    isEmergencyStopped: Boolean,
    onEmergencyStopToggle: () -> Unit,
    onOpenDemoTrade: (symbol: String, direction: String, entryPrice: Double) -> Unit,
    onCloseActiveTrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(EmatBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("trades_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Institutional Rule & Safety Banner
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
                                text = "CORE RISK PROTOCOL",
                                color = EmatTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "ONE-TRADE RULE",
                                color = EmatTextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.3).sp
                            )
                        }

                        StatusBadge(status = if (activeTrade != null) "1 / 1 (CAPACITY)" else "0 / 1 (AVAILABLE)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "EMAT AutonomIQ strictly prohibits multiple simultaneous active positions. Maximum active trades allowed across the entire universe is strictly 1. Any attempt to open a concurrent trade is automatically rejected by the Risk Engine.",
                        color = EmatTextSubtle,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Broker Adapter Connection State Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, EmatBorderHairline, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EXECUTION ADAPTER",
                            color = EmatTextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "MetaTrader 5 (MT5)",
                            color = EmatTextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    StatusBadge(status = "NOT CONNECTED")
                }
            }
        }

        // Active Trade Detail or Empty State
        item {
            if (activeTrade == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, EmatBorderHairline, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmatLongGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "NO ACTIVE POSITION",
                            color = EmatTextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Risk allocation: 0% utilized. System is ready to accept the single highest-conviction setup verified by the Scanner & ECE engine.",
                            color = EmatTextOffset,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                // Active Trade Card
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeTrade.symbol,
                                        color = EmatTextWhite,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = EmatFontFamily
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val isBuy = activeTrade.direction == TradeDirection.BUY
                                    StatusBadge(status = if (isBuy) "BUY" else "SELL")
                                }
                                Text(
                                    text = "ID: ${activeTrade.tradeId} | Mode: ${activeTrade.executionMode}",
                                    color = EmatTextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = EmatFontFamily
                                )
                            }

                            StatusBadge(status = activeTrade.status)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Price and Risk Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = EmatSurfaceActive
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("ENTRY PRICE", color = EmatTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(String.format("%.5f", activeTrade.entryPrice), color = EmatTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = EmatFontFamily)
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = EmatSurfaceActive
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("CURRENT PRICE", color = EmatTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(String.format("%.5f", activeTrade.currentPrice), color = EmatTechCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = EmatFontFamily)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = EmatSurfaceActive
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("STOP LOSS", color = EmatShortRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(String.format("%.5f", activeTrade.stopLoss), color = EmatTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = EmatFontFamily)
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = EmatSurfaceActive
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("TAKE PROFIT", color = EmatLongGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(String.format("%.5f", activeTrade.takeProfit), color = EmatTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = EmatFontFamily)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Trade Actions
                        Button(
                            onClick = onCloseActiveTrade,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("close_active_trade_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmatShortRedDim,
                                contentColor = Color(0xFFFCA5A5)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmatShortRedBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("CLOSE POSITION & JOURNAL EXIT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // One-Trade Rule Verification & Testing Section (Debug-only)
        if (com.example.BuildConfig.DEBUG) {
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
                            text = "ONE-TRADE RULE VERIFICATION TESTER (DEBUG ONLY)",
                            color = EmatTextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Click to execute a simulated demo position. If 1 trade is already open, pressing this tests the Risk Engine rejection logic and records the blocked violation to the audit log.",
                            color = EmatTextSubtle,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onOpenDemoTrade("EURUSD", "BUY", 1.0850) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("test_demo_trade_eurusd"),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmatBorderSubtle)
                            ) {
                                Text("BUY EURUSD (TEST DATA)", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = EmatTextWhite)
                            }

                            OutlinedButton(
                                onClick = { onOpenDemoTrade("XAUUSD", "SELL", 2420.50) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("test_demo_trade_xauusd"),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmatBorderSubtle)
                            ) {
                                Text("SELL GOLD (TEST DATA)", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = EmatTextWhite)
                            }
                        }
                    }
                }
            }
        }

        // Prominent Emergency Stop Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, if (isEmergencyStopped) EmatShortRedBorder else EmatBorderHairline, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = if (isEmergencyStopped) EmatShortRedDim else EmatSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isEmergencyStopped) EmatShortRed else EmatWarningAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "EMERGENCY PLATFORM HALT",
                                color = EmatTextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        StatusBadge(status = if (isEmergencyStopped) "ENGAGED" else "DISENGAGED")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Immediately closes all active positions, cancels pending execution adapters, and locks scanner evaluation into fail-safe mode.",
                        color = EmatTextSubtle,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onEmergencyStopToggle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("emergency_stop_toggle_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEmergencyStopped) EmatTechCyan else EmatShortRed,
                            contentColor = EmatTextWhite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isEmergencyStopped) "RESET EMERGENCY STOP" else "ENGAGE EMERGENCY STOP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

