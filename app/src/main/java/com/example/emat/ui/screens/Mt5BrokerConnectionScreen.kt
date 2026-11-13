package com.example.emat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emat.domain.model.*
import com.example.emat.ui.theme.*

@Composable
fun Mt5BrokerConnectionScreen(
    brokerConfig: Mt5BrokerConfig,
    bridgeDiagnostic: String,
    comparisonQuotes: List<Mt5InstrumentComparison>,
    onSaveConfig: (brokerName: String, brokerServer: String, accountNumber: String, accountType: String, investorPassword: String, bridgeUrl: String, bridgeToken: String) -> Unit,
    onTestConnection: () -> Unit,
    onConnectMt5: (brokerName: String, brokerServer: String, accountNumber: String, accountType: String, investorPassword: String, bridgeUrl: String, bridgeToken: String) -> Unit,
    onRunAutomaticWorkflow: (investorPassword: String) -> Unit = {},
    onAutoDetectBridge: () -> Unit = {},
    onDisconnect: () -> Unit,
    onRefresh: () -> Unit,
    onDeleteConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanName = if (brokerConfig.brokerName.contains("HFM", ignoreCase = true) || brokerConfig.brokerName.isBlank()) "XM Global" else brokerConfig.brokerName
    val cleanServer = if (brokerConfig.brokerServer.contains("HFM", ignoreCase = true) || brokerConfig.brokerServer.isBlank()) "XMGlobal-MT5 10" else brokerConfig.brokerServer
    val cleanAccount = if (brokerConfig.accountNumber == "55091351" || brokerConfig.accountNumber.isBlank()) "346533676" else brokerConfig.accountNumber
    val cleanType = if (brokerConfig.accountType.isBlank()) "DEMO" else brokerConfig.accountType

    var brokerName by remember(cleanName) { mutableStateOf(cleanName) }
    var brokerServer by remember(cleanServer) { mutableStateOf(cleanServer) }
    var accountNumber by remember(cleanAccount) { mutableStateOf(cleanAccount) }
    var accountType by remember(cleanType) { mutableStateOf(cleanType) }
    var investorPassword by remember(brokerConfig.investorPasswordMasked) {
        mutableStateOf(if (brokerConfig.investorPasswordMasked) "••••••••••••••" else "")
    }
    var bridgeUrl by remember(brokerConfig.bridgeUrl) { mutableStateOf(brokerConfig.bridgeUrl) }
    var bridgeToken by remember(brokerConfig.bridgeTokenMasked) {
        mutableStateOf(if (brokerConfig.bridgeTokenMasked) "••••••••••••••••••••••••••••••••" else "")
    }
    var showManualOverride by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EmatBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("mt5_broker_connection_screen")
    ) {
        // Header
        Text(
            text = "MT5 BROKER CONNECTION CENTRE",
            color = EmatTechCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            fontFamily = EmatFontFamily
        )
        Text(
            text = "Personal Broker Bridge & Terminal Feed",
            color = EmatTextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Read-Only investor credentials encrypted inside hardware-backed Android Keystore vault.",
            color = EmatTextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )

        // CONNECTION DASHBOARD CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .testTag("mt5_connection_dashboard")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONNECTION DASHBOARD",
                        color = EmatTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Bridge Status Badge
                    val bridgeBadgeColor = when (brokerConfig.bridgeStatus) {
                        BridgeStatus.CONNECTED -> EmatLongGreen
                        BridgeStatus.CONFIGURED -> EmatWarningAmber
                        BridgeStatus.NOT_CONFIGURED -> EmatShortRed
                    }
                    Surface(
                        color = bridgeBadgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, bridgeBadgeColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = brokerConfig.bridgeStatus.label,
                            color = bridgeBadgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dashboard Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DashboardMetric(label = "BROKER", value = brokerName.ifBlank { "XM Global" })
                    DashboardMetric(label = "SERVER", value = brokerServer.ifBlank { "XMGlobal-MT5 10" })
                    DashboardMetric(label = "ACCOUNT", value = "${accountNumber.ifBlank { "346533676" }} ($accountType)")
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = EmatBorderHairline, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DashboardMetric(
                        label = "BROKER AUTH STATUS",
                        value = brokerConfig.brokerAuthStatus.label,
                        valueColor = if (brokerConfig.brokerAuthStatus == BrokerAuthStatus.AUTHENTICATED) EmatLongGreen else EmatTextPrimary
                    )
                    DashboardMetric(
                        label = "MARKET DATA STATUS",
                        value = brokerConfig.marketDataStatus.label,
                        valueColor = if (brokerConfig.marketDataStatus == MarketDataFeedStatus.VALIDATED) EmatLongGreen else EmatTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DashboardMetric(label = "LAST SUCCESSFUL CONNECTION", value = brokerConfig.lastSuccessfulConnection)
                    DashboardMetric(label = "LAST RECEIVED TICK", value = brokerConfig.lastReceivedTick)
                    val instText = if (brokerConfig.availableInstrumentsCount > 0) "${brokerConfig.availableInstrumentsCount} Instruments" else "--"
                    DashboardMetric(label = "AVAILABLE INSTRUMENTS", value = instText)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Diagnostic: $bridgeDiagnostic",
                    color = EmatTechCyan,
                    fontSize = 11.sp,
                    fontFamily = EmatFontFamily
                )
            }
        }

        // WORKFLOW STEP PROGRESS CARD (When step > 0)
        if (brokerConfig.currentWorkflowStep > 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = EmatSurfaceActive),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmatTechCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AUTOMATIC CONNECTION WORKFLOW",
                            color = EmatTechCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${brokerConfig.currentWorkflowStep}/9",
                            color = EmatTextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = brokerConfig.workflowStepDescription,
                        color = EmatTextWhite,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Section 1: BROKER CONFIGURATION (PREFILLED)
        SectionHeader("1. BROKER CONFIGURATION (PREFILLED — READ-ONLY ACCESS)")
        OutlinedTextField(
            value = brokerName,
            onValueChange = { brokerName = it },
            label = { Text("Broker Name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("mt5_broker_name_input")
        )

        OutlinedTextField(
            value = brokerServer,
            onValueChange = { brokerServer = it },
            label = { Text("Broker Server") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("mt5_broker_server_input")
        )

        OutlinedTextField(
            value = accountNumber,
            onValueChange = { accountNumber = it },
            label = { Text("MT5 Account Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("mt5_account_number_input")
        )

        OutlinedTextField(
            value = accountType,
            onValueChange = { accountType = it },
            label = { Text("Account Type") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("mt5_account_type_input")
        )

        OutlinedTextField(
            value = investorPassword,
            onValueChange = { updated ->
                investorPassword = if (investorPassword == "••••••••••••••" && updated.length > "••••••••••••••".length) {
                    updated.removePrefix("••••••••••••••")
                } else if (investorPassword == "••••••••••••••" && updated.length < "••••••••••••••".length) {
                    ""
                } else {
                    updated
                }
            },
            label = {
                Text(
                    if (brokerConfig.investorPasswordMasked && investorPassword.all { it == '•' || it == '*' }) {
                        "Investor Password (Masked • Encrypted in Keystore Vault)"
                    } else {
                        "Investor Password (Read-Only)"
                    }
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            trailingIcon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Investor password masked and encrypted",
                    tint = EmatTechCyan
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("mt5_investor_password_input")
        )
        Text(
            text = "Security: Never enters standard trading password. Investor password grants strictly read-only price access.",
            color = EmatTextMuted,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Section 2: MT5 BRIDGE CONFIGURATION (AUTOMATIC POPULATION)
        SectionHeader("2. MT5 BRIDGE CONFIGURATION (AUTO-CONFIGURED)")

        // Auto-configuration banner
        Surface(
            color = EmatSurfaceActive,
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, EmatBorderHairline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AUTOMATIC BRIDGE REGISTRATION",
                            color = EmatTechCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        val addressDisplay = if (bridgeUrl.isNotBlank()) bridgeUrl else "Scanning for deployed bridge..."
                        Text(
                            text = "Deployed Address: $addressDisplay",
                            color = EmatTextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Authentication: Bearer Token encrypted in Android Keystore vault",
                            color = EmatTextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Button(
                        onClick = onAutoDetectBridge,
                        colors = ButtonDefaults.buttonColors(containerColor = EmatSurfaceCard),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("auto_detect_bridge_button")
                    ) {
                        Text("Detect Bridge", fontSize = 10.sp, color = EmatTechCyan)
                    }
                }
            }
        }

        // Optional Manual Override Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Manual Bridge Endpoint Override",
                color = EmatTextMuted,
                fontSize = 10.sp
            )
            TextButton(onClick = { showManualOverride = !showManualOverride }) {
                Text(if (showManualOverride) "Hide" else "Show Details", fontSize = 10.sp, color = EmatTechCyan)
            }
        }

        if (showManualOverride) {
            OutlinedTextField(
                value = bridgeUrl,
                onValueChange = { bridgeUrl = it },
                label = { Text("MT5 Bridge URL") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("mt5_bridge_url_input")
            )

            OutlinedTextField(
                value = bridgeToken,
                onValueChange = { updated ->
                    bridgeToken = if (bridgeToken == "••••••••••••••••••••••••••••••••" && updated.length > "••••••••••••••••••••••••••••••••".length) {
                        updated.removePrefix("••••••••••••••••••••••••••••••••")
                    } else if (bridgeToken == "••••••••••••••••••••••••••••••••" && updated.length < "••••••••••••••••••••••••••••••••".length) {
                        ""
                    } else {
                        updated
                    }
                },
                label = { Text(if (brokerConfig.bridgeTokenMasked && bridgeToken.all { it == '•' || it == '*' }) "Bridge Token (Masked • Auto-Configured in Keystore)" else "Bridge Authentication Token") },
                visualTransformation = PasswordVisualTransformation(),
                trailingIcon = {
                    Icon(
                        Icons.Default.VpnKey,
                        contentDescription = "Bridge token masked and encrypted",
                        tint = EmatTechCyan
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("mt5_bridge_token_input")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // PRIMARY ACTION: RUN AUTOMATIC CONNECTION (9-STEP WORKFLOW)
        Button(
            onClick = {
                onConnectMt5(brokerName, brokerServer, accountNumber, accountType, investorPassword, bridgeUrl, bridgeToken)
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmatTechCyan),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("run_auto_connect_button")
        ) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = EmatBackground, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RUN AUTOMATIC CONNECTION",
                color = EmatBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    onSaveConfig(brokerName, brokerServer, accountNumber, accountType, investorPassword, bridgeUrl, bridgeToken)
                    investorPassword = "••••••••••••••"
                    bridgeToken = "••••••••••••••••••••••••••••••••"
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_mt5_config_button")
            ) {
                Text("Save Config")
            }

            Button(
                onClick = onTestConnection,
                colors = ButtonDefaults.buttonColors(containerColor = EmatBorderHairline),
                modifier = Modifier.testTag("test_mt5_connection_button")
            ) {
                Text("Test Bridge")
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    onConnectMt5(brokerName, brokerServer, accountNumber, accountType, investorPassword, bridgeUrl, bridgeToken)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmatLongGreen),
                modifier = Modifier
                    .weight(1f)
                    .testTag("connect_mt5_button")
            ) {
                Text("Connect MT5")
            }

            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = EmatBorderHairline),
                modifier = Modifier.testTag("refresh_mt5_button")
            ) {
                Text("Refresh")
            }

            Button(
                onClick = onDisconnect,
                colors = ButtonDefaults.buttonColors(containerColor = EmatBorderHairline),
                modifier = Modifier.testTag("disconnect_mt5_button")
            ) {
                Text("Disconnect")
            }

            IconButton(
                onClick = onDeleteConfig,
                modifier = Modifier.testTag("delete_mt5_config_button")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete config", tint = EmatShortRed)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section 3: DATA SOURCE COMPARISON MATRIX (19 INSTRUMENTS)
        SectionHeader("3. DATA SOURCE COMPARISON MATRIX (19 INSTRUMENTS)")
        Text(
            text = "Compares live broker quotes from your connected terminal against biquote feed to verify price truth and spread fidelity.",
            color = EmatTextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (comparisonQuotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .border(1.dp, EmatBorderHairline, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap 'RUN AUTOMATIC CONNECTION' or 'Refresh' to poll broker & biquote comparison matrix.",
                    color = EmatTextMuted,
                    fontSize = 11.sp
                )
            }
        } else {
            comparisonQuotes.forEach { q ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${q.ematSymbol}  [Broker: ${q.brokerSymbol}]",
                                color = EmatTextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val tagColor = if (q.sourceStatus == "BROKER LIVE") EmatLongGreen else EmatTextMuted
                            Text(
                                text = q.sourceStatus,
                                color = tagColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val bBid = q.brokerBid?.toString() ?: "--"
                            val bAsk = q.brokerAsk?.toString() ?: "--"
                            val bSpread = q.brokerSpread?.let { "%.5f".format(it) } ?: "UNAVAILABLE"
                            Text(text = "Broker: $bBid / $bAsk (Spread: $bSpread)", color = EmatTextPrimary, fontSize = 11.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val bqBid = q.biquoteBid?.toString() ?: "--"
                            val bqAsk = q.biquoteAsk?.toString() ?: "--"
                            Text(text = "biquote: $bqBid / $bqAsk", color = EmatTextMuted, fontSize = 10.sp)
                            val diffText = q.priceDiff?.let { "Diff: %.5f".format(it) } ?: "Diff: --"
                            Text(text = diffText, color = EmatTechCyan, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardMetric(
    label: String,
    value: String,
    valueColor: Color = EmatTextWhite
) {
    Column {
        Text(
            text = label,
            color = EmatTextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = EmatTextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
    )
}
