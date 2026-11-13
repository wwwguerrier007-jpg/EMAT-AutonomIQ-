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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emat.domain.model.AccountEntitlement
import com.example.emat.domain.model.EmatAccount
import com.example.emat.domain.model.EmatNavDestination
import com.example.emat.domain.model.EntitlementState
import com.example.emat.ui.theme.EmatBackground
import com.example.emat.ui.theme.EmatBorderHairline
import com.example.emat.ui.theme.EmatFontFamily
import com.example.emat.ui.theme.EmatLongGreen
import com.example.emat.ui.theme.EmatShortRed
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
fun EmatSettingsScreen(
    currentDestination: EmatNavDestination,
    currentAccount: EmatAccount,
    currentAccountEntitlement: AccountEntitlement? = null,
    onStartTrial: () -> Unit = {},
    marketDataStatusLabel: String = "MARKET DATA: NOT CONNECTED",
    marketDataDiagnostic: String = "State: NOT_CONNECTED | Reason: -- | Last: --",
    onConnectLiveMarketData: () -> Unit = {},
    onTestMarketDataConnection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EmatBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SYSTEM & GOVERNANCE",
                    color = EmatTechCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    fontFamily = EmatFontFamily
                )
                Text(
                    text = currentDestination.displayName,
                    color = EmatTextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(EmatSurfaceElevated)
                    .border(1.dp, EmatBorderHairline, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "EMAT v1.0.4",
                    color = EmatTechCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (currentDestination) {
            EmatNavDestination.PROFILE -> {
                SettingsSectionHeader(title = "OPERATOR PROFILE")
                SettingFieldRow("Account Name", currentAccount.name)
                SettingFieldRow("Operator ID", currentAccount.id)
                SettingFieldRow("Account Category", currentAccount.category.displayName)
                SettingFieldRow("Role", currentAccount.role)
                SettingFieldRow("Tier", currentAccount.tier)
                SettingFieldRow("Daily Loss Limit", currentAccount.dailyLossLimit)
                SettingFieldRow("Position Ceiling", currentAccount.maxPositionSize)
                SettingFieldRow("Execution Permission", if (currentAccount.isLiveExecutionAllowed) "LIVE AUTHORIZED" else "SUPERVISED PAPER ONLY")
                SettingFieldRow("Tenancy Status", "Local Terminal Commissioning (Multi-User Cloud: In Foundation Phase)")
            }

            EmatNavDestination.SECURITY -> {
                SettingsSectionHeader(title = "SECURITY CONTROLS")
                SettingFieldRow("Authentication Gate", "SHA-256 Passcode + Biometric Prompt")
                SettingFieldRow("Biometrics Support", "Class 3 Strong Biometric / Fingerprint / Face")
                SettingFieldRow("Session Inactivity Timeout", "15 Minutes")
                SettingFieldRow("Database Encryption", "Local AES-256 Room Storage")
                SettingFieldRow("Audit Integrity", "Append-Only Immutable Logs (SHA-256 Tamper-Evident)")
            }

            EmatNavDestination.SOFTWARE_INFO -> {
                SettingsSectionHeader(title = "SOFTWARE ARCHITECTURE")
                SettingFieldRow("Platform", "EMAT AutonomIQ")
                SettingFieldRow("Ecosystem", "Autonomous — Product of UNIEE")
                SettingFieldRow("Build Version", "1.0.4")
                SettingFieldRow("Engine Architecture", "Multi-Factor ECE + Core Risk Guardrails")
                SettingFieldRow("One-Trade Rule Invariant", "Enforced with Repository Mutex Critical Section")
                SettingFieldRow("Market Data Feed", marketDataStatusLabel)
                SettingFieldRow("Market Data Provider", "MetaTrader 5 (via biquote.io)")
                SettingFieldRow("Broker Connectivity", "NOT_CONNECTED (Fail-Safe State)")
                SettingFieldRow("Database Durability", "SQLite Room Version 4 (Additive MIGRATION_3_4)")

                SettingsSectionHeader(title = "METATRADER 5 MARKET DATA (READ-ONLY)")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onConnectLiveMarketData() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("connect_market_data_button")
                    ) {
                        Text("Connect Live")
                    }
                    Button(
                        onClick = { onTestMarketDataConnection() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmatBorderHairline),
                        modifier = Modifier.testTag("test_connection_button")
                    ) {
                        Text("Test connection")
                    }
                }
                Text(
                    text = marketDataDiagnostic,
                    color = EmatTextMuted,
                    fontSize = 11.sp,
                    fontFamily = EmatFontFamily,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .testTag("market_data_diagnostic_line")
                )

                SettingsSectionHeader(title = "ENTITLEMENTS & BILLING SUBSYSTEM")
                SettingFieldRow("Billing Provider", "DefaultBillingProvider (NOT_CONNECTED)")
                val stateText = currentAccountEntitlement?.state?.name ?: "NOT_SUBSCRIBED"
                SettingFieldRow("Entitlement Status", stateText)
                SettingFieldRow("Plan ID", "UNAVAILABLE")
                val trialDuration = "${currentAccountEntitlement?.trialDurationDays ?: 14} Days (Configured)"
                SettingFieldRow("Trial Allocation", trialDuration)
                val remainingText = when {
                    currentAccountEntitlement?.hasClockAnomaly == true -> "CLOCK ANOMALY DETECTED"
                    currentAccountEntitlement?.state == EntitlementState.TRIAL_ACTIVE -> "${currentAccountEntitlement.remainingTrialDays} Days (${currentAccountEntitlement.remainingTrialHours}h)"
                    currentAccountEntitlement?.state == EntitlementState.TRIAL_EXPIRED -> "EXPIRED"
                    currentAccountEntitlement?.state == EntitlementState.SUBSCRIPTION_ACTIVE -> "ACTIVE (VERIFIED)"
                    currentAccountEntitlement?.state == EntitlementState.SUBSCRIPTION_EXPIRED -> "EXPIRED"
                    else -> "NOT STARTED"
                }
                SettingFieldRow("Access Window", remainingText)

                if (currentAccountEntitlement == null || currentAccountEntitlement.state == EntitlementState.NOT_SUBSCRIBED) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onStartTrial,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("settings_start_trial_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmatTechCyan,
                            contentColor = EmatTextWhite
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Start 14-Day Free Trial", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            EmatNavDestination.NOTIFICATIONS -> {
                SettingsSectionHeader(title = "TELEMETRY NOTIFICATIONS")
                SettingFieldRow("High-Impact News Alerts", "ENABLED (45m pre-event buffer)")
                SettingFieldRow("Risk Limit Breach Alerts", "ENABLED (Instant Alert)")
                SettingFieldRow("Scanner Execution Alerts", "ENABLED (ECE >= 70)")
                SettingFieldRow("Fail-Safe Circuit Breaker", "ENABLED (Visual + Audio Tone)")
            }

            else -> {
                SettingsSectionHeader(title = "PLATFORM SETTINGS")
                SettingFieldRow("Default Chart Timeframe", "H1 (Hourly)")
                SettingFieldRow("Market Scanner Mode", "Institutional Multi-Asset Universe")
                SettingFieldRow("Offline Handling", "Automatic Fail-Safe Halt")
                SettingFieldRow("Data Provider", "Normalized Real-Tick Engine (Demo/Test Feed)")
                SettingFieldRow("Audit Storage", "Room SQLite Database")
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = EmatTextMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingFieldRow(
    label: String,
    value: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(EmatSurfaceCard)
            .border(1.dp, EmatBorderHairline, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = EmatTextSubtle,
                fontSize = 12.sp
            )
            Text(
                text = value,
                color = EmatTextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
