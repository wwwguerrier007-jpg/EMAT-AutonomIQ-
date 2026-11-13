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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
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
import com.example.emat.domain.model.ActiveTrade
import com.example.emat.domain.model.EmatAccount
import com.example.emat.ui.theme.EmatBackground
import com.example.emat.ui.theme.EmatBorderHairline
import com.example.emat.ui.theme.EmatFontFamily
import com.example.emat.ui.theme.EmatLongGreen
import com.example.emat.ui.theme.EmatShortRed
import com.example.emat.ui.theme.EmatShortRedDim
import com.example.emat.ui.theme.EmatSurfaceCard
import com.example.emat.ui.theme.EmatSurfaceElevated
import com.example.emat.ui.theme.EmatTechCyan
import com.example.emat.ui.theme.EmatTextMuted
import com.example.emat.ui.theme.EmatTextPrimary
import com.example.emat.ui.theme.EmatTextSubtle
import com.example.emat.ui.theme.EmatTextWhite
import com.example.emat.ui.theme.EmatWarningAmber

@Composable
fun EmatRiskScreen(
    currentAccount: EmatAccount,
    activeTrade: ActiveTrade?,
    isEmergencyStopped: Boolean,
    onEmergencyStopToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EmatBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("risk_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CORE RISK PROTOCOL",
                    color = EmatTechCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    fontFamily = EmatFontFamily
                )
                Text(
                    text = "Risk Management Engine",
                    color = EmatTextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isEmergencyStopped) Color(0x22EF4444) else Color(0x2210B981))
                    .border(
                        1.dp,
                        if (isEmergencyStopped) EmatShortRed else EmatLongGreen,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isEmergencyStopped) "SYSTEM HALTED" else "PROTECTION ARMED",
                    color = if (isEmergencyStopped) EmatShortRed else EmatLongGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Verified Risk Exposure Chart (Actual Data Only)
        com.example.emat.ui.components.EmatRiskExposureChart(
            activeTrade = activeTrade,
            account = currentAccount
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Rule #1 Invariant Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(EmatSurfaceCard)
                .border(1.dp, EmatTechCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = EmatTechCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ONE-TRADE RULE (INVARIANT)",
                            color = EmatTechCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (activeTrade == null) Color(0x2210B981) else Color(0x22F59E0B))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (activeTrade == null) "0 / 1 ACTIVE" else "1 / 1 LOCKED",
                            color = if (activeTrade == null) EmatLongGreen else EmatWarningAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Strict invariant: Only ONE active trade is permitted across the entire platform at any time. Any new setup is automatically rejected while a position is open.",
                    color = EmatTextPrimary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Parameters Grid
        Text(
            text = "INSTITUTIONAL RISK BOUNDARIES",
            color = EmatTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        RiskParameterRow(title = "Daily Loss Limit", value = currentAccount.dailyLossLimit, status = "ENFORCED")
        RiskParameterRow(title = "Max Position Size", value = currentAccount.maxPositionSize, status = "CAPPED")
        RiskParameterRow(title = "Max Risk Per Trade", value = "1.0% Equity", status = "CALCULATED")
        RiskParameterRow(title = "Minimum Risk:Reward", value = "1 : 2.0 RR", status = "MANDATORY")
        RiskParameterRow(title = "Data Freshness Threshold", value = "< 15 Seconds", status = "ACTIVE")

        Spacer(modifier = Modifier.height(16.dp))

        // Emergency Circuit Breaker
        Text(
            text = "FAIL-SAFE CIRCUIT BREAKER",
            color = EmatTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isEmergencyStopped) EmatShortRedDim else EmatSurfaceCard)
                .border(
                    1.dp,
                    if (isEmergencyStopped) EmatShortRed else EmatBorderHairline,
                    RoundedCornerShape(10.dp)
                )
                .clickable { onEmergencyStopToggle() }
                .padding(14.dp)
                .testTag("risk_emergency_stop_toggle")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isEmergencyStopped) EmatShortRed else Color(0x22EF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Halt",
                        tint = if (isEmergencyStopped) Color.White else EmatShortRed,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEmergencyStopped) "EMERGENCY HALT ENGAGED" else "ENGAGE EMERGENCY STOP",
                        color = if (isEmergencyStopped) EmatShortRed else EmatTextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEmergencyStopped) "Tap to clear halt condition and resume monitoring" else "Instantly liquidate all active orders & block new executions",
                        color = EmatTextSubtle,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RiskParameterRow(
    title: String,
    value: String,
    status: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(EmatSurfaceCard)
            .border(1.dp, EmatBorderHairline, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    color = EmatTextSubtle,
                    fontSize = 11.sp
                )
                Text(
                    text = value,
                    color = EmatTextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(EmatSurfaceElevated)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = status,
                    color = EmatTechCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
