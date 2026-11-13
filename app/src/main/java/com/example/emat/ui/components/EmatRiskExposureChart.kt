package com.example.emat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emat.domain.model.ActiveTrade
import com.example.emat.domain.model.AssetClass
import com.example.emat.domain.model.EmatAccount
import com.example.emat.ui.theme.ElectricBlue
import com.example.emat.ui.theme.ElectricBlueBright
import com.example.emat.ui.theme.EmatBorderHairline
import com.example.emat.ui.theme.EmatBorderSubtle
import com.example.emat.ui.theme.EmatFontFamily
import com.example.emat.ui.theme.EmatLongGreen
import com.example.emat.ui.theme.EmatShortRed
import com.example.emat.ui.theme.EmatSurfaceCard
import com.example.emat.ui.theme.EmatSurfaceElevated
import com.example.emat.ui.theme.EmatTechCyan
import com.example.emat.ui.theme.EmatTextMuted
import com.example.emat.ui.theme.EmatTextOffset
import com.example.emat.ui.theme.EmatTextSubtle
import com.example.emat.ui.theme.EmatTextWhite
import com.example.emat.ui.theme.EmatWarningAmber
import java.util.Locale
import kotlin.math.abs

data class ExposureCalculation(
    val hasActiveTrade: Boolean,
    val openPositionExposurePercent: Double?,
    val potentialLossAmount: Double?,
    val potentialLossPercent: Double?,
    val assetClass: AssetClass?,
    val instrumentSymbol: String?,
    val accountEquity: Double?,
    val availableFunds: Double?,
    val configuredRiskLimits: String?,
    val isDataAvailable: Boolean,
    val statusText: String
)

object ExposureCalculator {
    fun calculate(
        activeTrade: ActiveTrade?,
        account: EmatAccount,
        accountEquity: Double? = null,
        availableFunds: Double? = null,
        configuredRiskLimits: String? = null
    ): ExposureCalculation {
        val resolvedRiskLimit = configuredRiskLimits ?: account.dailyLossLimit.takeIf { it.isNotBlank() }

        if (activeTrade == null || activeTrade.status != "ACTIVE") {
            return ExposureCalculation(
                hasActiveTrade = false,
                openPositionExposurePercent = null,
                potentialLossAmount = null,
                potentialLossPercent = null,
                assetClass = null,
                instrumentSymbol = null,
                accountEquity = accountEquity,
                availableFunds = availableFunds,
                configuredRiskLimits = resolvedRiskLimit,
                isDataAvailable = false,
                statusText = "NO ACTIVE EMAT TRADE"
            )
        }

        val symbol = activeTrade.symbol.uppercase(Locale.US)
        val assetClass = when {
            symbol.contains("XAU") || symbol.contains("XAG") || symbol.contains("GOLD") || symbol.contains("SILVER") -> AssetClass.METALS
            symbol.contains("NAS") || symbol.contains("US30") || symbol.contains("SPX") || symbol.contains("GER") -> AssetClass.INDICES
            symbol.contains("BTC") || symbol.contains("ETH") || symbol.contains("CRYPTO") -> AssetClass.CRYPTO
            else -> AssetClass.FOREX
        }

        val slDistance = abs(activeTrade.entryPrice - activeTrade.stopLoss)
        if (activeTrade.stopLoss <= 0.0 || slDistance <= 0.0) {
            return ExposureCalculation(
                hasActiveTrade = true,
                openPositionExposurePercent = null,
                potentialLossAmount = null,
                potentialLossPercent = null,
                assetClass = assetClass,
                instrumentSymbol = symbol,
                accountEquity = accountEquity,
                availableFunds = availableFunds,
                configuredRiskLimits = resolvedRiskLimit,
                isDataAvailable = false,
                statusText = "INSUFFICIENT DATA: STOP-LOSS NOT SPECIFIED"
            )
        }

        val contractMultiplier = when (assetClass) {
            AssetClass.FOREX -> 100000.0
            AssetClass.METALS -> if (symbol.contains("XAG")) 5000.0 else 100.0
            AssetClass.INDICES -> 1.0
            AssetClass.CRYPTO -> 1.0
        }

        val rawLoss = slDistance * activeTrade.positionSizeLots * contractMultiplier
        val lossPercent = if (accountEquity != null && accountEquity > 0.0) {
            (rawLoss / accountEquity) * 100.0
        } else {
            null
        }

        return ExposureCalculation(
            hasActiveTrade = true,
            openPositionExposurePercent = lossPercent?.coerceIn(0.0, 10.0),
            potentialLossAmount = rawLoss,
            potentialLossPercent = lossPercent?.coerceIn(0.0, 10.0),
            assetClass = assetClass,
            instrumentSymbol = symbol,
            accountEquity = accountEquity,
            availableFunds = availableFunds,
            configuredRiskLimits = resolvedRiskLimit,
            isDataAvailable = true,
            statusText = "${activeTrade.direction} $symbol (${String.format(Locale.US, "%.2f", activeTrade.positionSizeLots)} Lots)"
        )
    }
}

@Composable
fun EmatRiskExposureChart(
    activeTrade: ActiveTrade?,
    account: EmatAccount,
    modifier: Modifier = Modifier
) {
    val calculation = ExposureCalculator.calculate(activeTrade, account)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, EmatBorderHairline, RoundedCornerShape(16.dp))
            .testTag("risk_exposure_chart_card"),
        colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RISK EXPOSURE MONITOR",
                        color = EmatTechCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = EmatFontFamily
                    )
                    Text(
                        text = "Portfolio Risk Exposure",
                        color = EmatTextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (calculation.hasActiveTrade) Color(0x22F59E0B) else Color(0x2210B981))
                        .border(
                            1.dp,
                            if (calculation.hasActiveTrade) EmatWarningAmber else EmatLongGreen,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (calculation.hasActiveTrade) "EXPOSURE ACTIVE" else "NO ACTIVE TRADE",
                        color = if (calculation.hasActiveTrade) EmatWarningAmber else EmatLongGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Central Canvas Gauge: Semi-Circular Meter
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                val exposureFraction = if (calculation.potentialLossPercent != null) {
                    (calculation.potentialLossPercent / 1.0).toFloat().coerceIn(0f, 1f)
                } else 0f

                Canvas(
                    modifier = Modifier
                        .size(width = 200.dp, height = 110.dp)
                        .testTag("exposure_gauge_canvas")
                ) {
                    val strokeWidthPx = 14.dp.toPx()
                    val arcSize = Size(size.width - strokeWidthPx, size.height * 1.8f - strokeWidthPx)
                    val arcOffset = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)

                    // Track Arc (Background 180 degrees)
                    drawArc(
                        color = Color(0x22FFFFFF),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = arcOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )

                    // Active Sweep Arc (if any exposure)
                    if (calculation.hasActiveTrade && calculation.isDataAvailable && exposureFraction > 0f) {
                        val arcColor = when {
                            (calculation.potentialLossPercent ?: 0.0) <= 0.5 -> EmatLongGreen
                            (calculation.potentialLossPercent ?: 0.0) <= 0.8 -> EmatWarningAmber
                            else -> EmatShortRed
                        }

                        drawArc(
                            color = arcColor,
                            startAngle = 180f,
                            sweepAngle = 180f * exposureFraction,
                            useCenter = false,
                            topLeft = arcOffset,
                            size = arcSize,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                        )
                    }
                }

                // Center Value Readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 22.dp)
                ) {
                    if (!calculation.hasActiveTrade) {
                        Text(
                            text = "NO ACTIVE EMAT TRADE",
                            color = EmatTextSubtle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontFamily = EmatFontFamily
                        )
                        Text(
                            text = "Capital at zero position risk",
                            color = EmatTextMuted,
                            fontSize = 10.sp
                        )
                    } else if (!calculation.isDataAvailable) {
                        Text(
                            text = "DATA N/A",
                            color = EmatWarningAmber,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = EmatFontFamily
                        )
                        Text(
                            text = calculation.statusText,
                            color = EmatTextMuted,
                            fontSize = 10.sp
                        )
                    } else if (calculation.potentialLossPercent != null) {
                        Text(
                            text = String.format(Locale.US, "%.2f%%", calculation.potentialLossPercent),
                            color = if (calculation.potentialLossPercent > 0.8) EmatShortRed else EmatTextWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = EmatFontFamily
                        )
                        Text(
                            text = "Potential Stop-Loss Risk",
                            color = EmatTextSubtle,
                            fontSize = 10.sp
                        )
                    } else {
                        Text(
                            text = "-$" + String.format(Locale.US, "%.2f", calculation.potentialLossAmount ?: 0.0),
                            color = EmatTextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = EmatFontFamily
                        )
                        Text(
                            text = "SL Potential Loss (Equity Unavailable)",
                            color = EmatTextSubtle,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // 5 Mandatory Source Metrics Detail Grid (Truthful fallbacks to UNAVAILABLE)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Equity & Available Funds
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        label = "ACCOUNT EQUITY",
                        value = calculation.accountEquity?.let { "$" + String.format(Locale.US, "%.2f", it) } ?: "UNAVAILABLE",
                        sub = if (calculation.accountEquity != null) "Verified Broker Book" else "Source Not Connected",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "AVAILABLE FUNDS",
                        value = calculation.availableFunds?.let { "$" + String.format(Locale.US, "%.2f", it) } ?: "UNAVAILABLE",
                        sub = if (calculation.availableFunds != null) "Free Margin" else "Source Not Connected",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Configured Risk Limits & Stop-Loss Potential Loss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        label = "CONFIGURED RISK LIMITS",
                        value = calculation.configuredRiskLimits ?: "UNAVAILABLE",
                        sub = if (calculation.configuredRiskLimits != null) "Account Envelope" else "Not Configured",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "STOP-LOSS POTENTIAL LOSS",
                        value = if (calculation.hasActiveTrade && calculation.potentialLossAmount != null)
                            "-$" + String.format(Locale.US, "%.2f", calculation.potentialLossAmount)
                        else "UNAVAILABLE",
                        sub = if (calculation.hasActiveTrade) (calculation.instrumentSymbol ?: "Active Position") else "No Position Open",
                        modifier = Modifier.weight(1f),
                        isHighlight = calculation.hasActiveTrade
                    )
                }
            }

            // Asset-Class Segmented Exposure Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ASSET CLASS EXPOSURE",
                        color = EmatTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (calculation.hasActiveTrade && calculation.assetClass != null) calculation.assetClass.displayName else "UNAVAILABLE",
                        color = if (calculation.hasActiveTrade) EmatTechCyan else EmatTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0x14FFFFFF)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    AssetClass.values().forEach { ac ->
                        val isMatched = calculation.hasActiveTrade && calculation.assetClass == ac
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isMatched) ElectricBlue else Color.Transparent
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AssetClass.values().forEach { ac ->
                        val isMatched = calculation.hasActiveTrade && calculation.assetClass == ac
                        Text(
                            text = ac.displayName,
                            color = if (isMatched) ElectricBlueBright else EmatTextOffset,
                            fontSize = 9.sp,
                            fontWeight = if (isMatched) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(EmatSurfaceElevated)
            .border(1.dp, EmatBorderHairline, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, color = EmatTextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text(
                text = value,
                color = if (isHighlight) EmatWarningAmber else EmatTextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = EmatFontFamily
            )
            Text(text = sub, color = EmatTextOffset, fontSize = 8.sp)
        }
    }
}
