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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emat.data.local.entity.TradeEntity
import com.example.emat.domain.model.PerformanceMetrics
import com.example.emat.ui.theme.EmatBackground
import com.example.emat.ui.theme.EmatBorderHairline
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EmatPerformanceScreen(
    metrics: PerformanceMetrics,
    trades: List<TradeEntity>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EmatBackground)
            .padding(16.dp)
            .testTag("performance_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "QUANTITATIVE AUDIT",
                    color = EmatTechCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    fontFamily = EmatFontFamily
                )
                Text(
                    text = "Performance & Risk Expectancy",
                    color = EmatTextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (metrics.isStatisticallySignificant) Color(0x2210B981) else Color(0x22F59E0B))
                    .border(
                        1.dp,
                        if (metrics.isStatisticallySignificant) EmatLongGreen else EmatWarningAmber,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (metrics.isStatisticallySignificant) "STATISTICALLY VALIDATED" else "SAMPLE BUILDING (${metrics.totalTrades}/30)",
                    color = if (metrics.isStatisticallySignificant) EmatLongGreen else EmatWarningAmber,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sample Size Governance Notice
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(EmatSurfaceCard)
                .border(1.dp, EmatBorderHairline, RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = EmatTechCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = metrics.notes,
                    color = EmatTextPrimary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Metric Grid (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricStatCard(
                title = "WIN RATE",
                value = "${String.format(Locale.US, "%.1f", metrics.winRatePercent)}%",
                subtitle = "${metrics.winningTrades}W - ${metrics.losingTrades}L",
                color = if (metrics.winRatePercent >= 50.0) EmatLongGreen else EmatWarningAmber,
                modifier = Modifier.weight(1f)
            )
            MetricStatCard(
                title = "PROFIT FACTOR",
                value = String.format(Locale.US, "%.2f", metrics.profitFactor),
                subtitle = if (metrics.profitFactor >= 1.5) "Institutional Grade" else "Evaluating",
                color = if (metrics.profitFactor >= 1.5) EmatLongGreen else EmatTechCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricStatCard(
                title = "MAX DRAWDOWN",
                value = "${String.format(Locale.US, "%.1f", metrics.maxDrawdownPercent)}%",
                subtitle = "Hard Limit: 2.0%",
                color = if (metrics.maxDrawdownPercent < 2.0) EmatLongGreen else EmatShortRed,
                modifier = Modifier.weight(1f)
            )
            MetricStatCard(
                title = "NET REALIZED PNL",
                value = "${if (metrics.netPnL >= 0) "+" else ""}$${String.format(Locale.US, "%.2f", metrics.netPnL)}",
                subtitle = "Audited Trades: ${metrics.totalTrades}",
                color = if (metrics.netPnL >= 0) EmatLongGreen else EmatShortRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "AUDITED TRADE EXECUTIONS",
            color = EmatTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (trades.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(EmatSurfaceCard)
                    .border(1.dp, EmatBorderHairline, RoundedCornerShape(8.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No trades executed yet. All trades will be permanently recorded here with strict audit logs.",
                    color = EmatTextSubtle,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trades) { trade ->
                    TradeAuditRow(trade = trade)
                }
            }
        }
    }
}

@Composable
private fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(EmatSurfaceCard)
            .border(1.dp, EmatBorderHairline, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = EmatTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = value,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = EmatFontFamily
            )
            Text(
                text = subtitle,
                color = EmatTextSubtle,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun TradeAuditRow(trade: TradeEntity) {
    val dateStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(trade.openedAt))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(EmatSurfaceCard)
            .border(1.dp, EmatBorderHairline, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = trade.symbol,
                        color = EmatTextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (trade.direction == "BUY") Color(0x2210B981) else Color(0x22EF4444))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = trade.direction,
                            color = if (trade.direction == "BUY") EmatLongGreen else EmatShortRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${trade.positionSize} Lots",
                        color = EmatTextSubtle,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Entry: ${trade.entryPrice} • SL: ${trade.stopLoss} • TP: ${trade.takeProfit}",
                    color = EmatTextMuted,
                    fontSize = 10.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (trade.unrealizedResult >= 0) "+" else ""}$${String.format(Locale.US, "%.2f", trade.unrealizedResult)}",
                    color = if (trade.unrealizedResult >= 0) EmatLongGreen else EmatShortRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = EmatFontFamily
                )
                Text(
                    text = "${trade.status} • $dateStr",
                    color = EmatTextSubtle,
                    fontSize = 9.sp
                )
            }
        }
    }
}
