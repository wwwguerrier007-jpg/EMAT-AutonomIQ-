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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emat.domain.service.MarketDataState
import com.example.emat.ui.theme.EmatFontFamily
import com.example.emat.data.local.entity.InstrumentEntity
import com.example.emat.domain.model.Opportunity
import com.example.emat.domain.model.TradeDirection
import com.example.emat.ui.components.OpportunityCard
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
fun ScannerScreen(
    isScanning: Boolean,
    scannerStatus: String,
    lastScanTime: String,
    instruments: List<InstrumentEntity>,
    topOpportunities: List<Opportunity>,
    selectedOpportunity: Opportunity?,
    onTriggerScan: () -> Unit,
    onSelectOpportunity: (Opportunity?) -> Unit,
    onToggleInstrument: (String, Boolean) -> Unit,
    onAddInstrument: ((symbol: String, name: String, assetClass: String, pipSize: Double, digits: Int) -> Unit)? = null,
    onRemoveInstrument: ((symbol: String) -> Unit)? = null,
    marketDataState: MarketDataState = MarketDataState.NOT_CONNECTED,
    modifier: Modifier = Modifier
) {
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var isAddInstrumentDialogOpen by remember { mutableStateOf(false) }

    var newSymbol by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newAssetClass by remember { mutableStateOf("FOREX") }
    var newPipSize by remember { mutableStateOf("0.0001") }
    var newDigits by remember { mutableStateOf("5") }

    val filteredInstruments = remember(instruments, selectedCategoryFilter) {
        if (selectedCategoryFilter == "ALL") instruments
        else instruments.filter { it.assetClass == selectedCategoryFilter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(EmatBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("scanner_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Scanner Trigger & Metric Header
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
                                text = "MARKET UNIVERSE SCANNER",
                                color = EmatTextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = "Dynamic Multi-Asset Scanning & ECE Ranking",
                                color = EmatTextOffset,
                                fontSize = 11.sp
                            )
                        }

                        StatusBadge(status = scannerStatus)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ACTIVE ASSETS", color = EmatTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Text("${instruments.count { it.isActive }} / ${instruments.size}", color = EmatTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = EmatFontFamily)
                        }
                        Column {
                            Text("QUALIFIED", color = EmatTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Text("${topOpportunities.count { it.symbol != "NONE" }} Setups", color = EmatLongGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = EmatFontFamily)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("LAST RUN", color = EmatTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Text(lastScanTime, color = EmatTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = EmatFontFamily)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onTriggerScan,
                        enabled = !isScanning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("scanner_action_button"),
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
                            Text("SCANNING ACTIVE UNIVERSE...", fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SCAN ALL INSTRUMENTS", fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }
        }

        // Section: Top 3 Opportunities
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
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
                    text = "STRICT ECE RANK",
                    color = EmatTechCyan,
                    fontSize = 10.sp,
                    fontFamily = EmatFontFamily
                )
            }
        }

        // Top 3 Cards
        if (marketDataState == MarketDataState.NOT_CONNECTED) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, EmatWarningAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "MARKET DATA: NOT CONNECTED",
                            color = EmatWarningAmber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No synthetic or fabricated setups generated. Connect a validated institutional market data provider to scan.",
                            color = EmatTextOffset,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        } else if (topOpportunities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, EmatBorderHairline, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No active scan performed yet.", color = EmatTextSubtle, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Click 'SCAN ALL INSTRUMENTS' to generate institutional ratings.", color = EmatTextOffset, fontSize = 11.sp)
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
                    onClick = { onSelectOpportunity(opp) }
                )
            }
        }

        // Section: Configured Market Universe (Forex, Metals, Indices, Crypto)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CONFIGURED MARKET UNIVERSE",
                            color = EmatTextSubtle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${instruments.size} Registered Instruments across Asset Classes",
                            color = EmatTextOffset,
                            fontSize = 11.sp
                        )
                    }

                    if (onAddInstrument != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmatTechCyan.copy(alpha = 0.15f))
                                .border(1.dp, EmatTechCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { isAddInstrumentDialogOpen = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("add_custom_asset_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Asset", tint = EmatTechCyan, modifier = Modifier.size(14.dp))
                                Text("ADD ASSET", color = EmatTechCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Asset Class Category Filters
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val categories = listOf("ALL", "FOREX", "METALS", "INDICES", "CRYPTO")
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text(cat, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmatTechCyan,
                                selectedLabelColor = EmatTextWhite,
                                containerColor = EmatSurfaceCard,
                                labelColor = EmatTextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedCategoryFilter == cat,
                                borderColor = if (selectedCategoryFilter == cat) EmatTechCyan else EmatBorderHairline
                            ),
                            shape = RoundedCornerShape(50)
                        )
                    }
                }
            }
        }

        // List of Instruments in Universe
        items(filteredInstruments) { inst ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, EmatBorderHairline, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = inst.symbol,
                                color = EmatTextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = EmatFontFamily
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(status = inst.assetClass)
                        }
                        Text(
                            text = inst.name,
                            color = EmatTextOffset,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Pip: ${inst.pipSize} | Contract: ${inst.contractSize.toInt()}",
                            color = EmatTextMuted,
                            fontSize = 10.sp,
                            fontFamily = EmatFontFamily
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = inst.isActive,
                            onCheckedChange = { onToggleInstrument(inst.symbol, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmatTextWhite,
                                checkedTrackColor = EmatLongGreen,
                                uncheckedThumbColor = EmatTextMuted,
                                uncheckedTrackColor = EmatSurfaceElevated
                            ),
                            modifier = Modifier.testTag("toggle_instrument_${inst.symbol}")
                        )

                        if (onRemoveInstrument != null && instruments.size > 5) {
                            IconButton(
                                onClick = { onRemoveInstrument(inst.symbol) },
                                modifier = Modifier.size(32.dp).testTag("delete_instrument_${inst.symbol}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Instrument",
                                    tint = EmatTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
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

    // Opportunity Detail Dialog
    if (selectedOpportunity != null && selectedOpportunity.symbol != "NONE") {
        AlertDialog(
            onDismissRequest = { onSelectOpportunity(null) },
            confirmButton = {
                Button(
                    onClick = { onSelectOpportunity(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmatTechCyan, contentColor = EmatTextWhite),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CLOSE EVIDENCE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedOpportunity.symbol} AUDIT EVIDENCE",
                        color = EmatTextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = EmatFontFamily
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rank: #${selectedOpportunity.rank}", color = EmatTextMuted, fontSize = 12.sp)
                        Text("Direction: ${selectedOpportunity.direction.name}", color = if (selectedOpportunity.direction == TradeDirection.BUY) EmatLongGreen else EmatShortRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ECE Score: ${selectedOpportunity.eceScore}/100", color = EmatTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = EmatFontFamily)
                        StatusBadge(status = selectedOpportunity.setupStatus.label)
                    }

                    Text("Timeframe Alignment: ${selectedOpportunity.timeframeAlignment.label}", color = EmatTechCyan, fontSize = 11.sp)
                    Text("Market Condition: ${selectedOpportunity.marketCondition.label}", color = EmatTextSubtle, fontSize = 11.sp)
                    Text("Risk Verification: ${selectedOpportunity.riskStatus.label}", color = EmatLongGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Qualification Evidence Rationale:", color = EmatTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = selectedOpportunity.whyItQualifies,
                        color = EmatTextWhite,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            containerColor = EmatSurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Add Custom Asset Dialog
    if (isAddInstrumentDialogOpen) {
        AlertDialog(
            onDismissRequest = { isAddInstrumentDialogOpen = false },
            title = {
                Text(
                    text = "REGISTER UNIVERSE INSTRUMENT",
                    color = EmatTextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = EmatFontFamily
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Add a verified asset to the institutional scanner registry.",
                        color = EmatTextOffset,
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = newSymbol,
                        onValueChange = { newSymbol = it.uppercase() },
                        label = { Text("Symbol (e.g. EURGBP)", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = EmatTextWhite,
                            unfocusedTextColor = EmatTextWhite,
                            focusedBorderColor = EmatTechCyan,
                            unfocusedBorderColor = EmatBorderHairline
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_new_asset_symbol")
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Asset Name (e.g. Euro / British Pound)", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = EmatTextWhite,
                            unfocusedTextColor = EmatTextWhite,
                            focusedBorderColor = EmatTechCyan,
                            unfocusedBorderColor = EmatBorderHairline
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_new_asset_name")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val classes = listOf("FOREX", "METALS", "INDICES", "CRYPTO")
                        classes.forEach { cls ->
                            FilterChip(
                                selected = newAssetClass == cls,
                                onClick = {
                                    newAssetClass = cls
                                    if (cls == "FOREX") {
                                        newPipSize = "0.0001"
                                        newDigits = "5"
                                    } else if (cls == "METALS") {
                                        newPipSize = "0.01"
                                        newDigits = "2"
                                    } else if (cls == "INDICES") {
                                        newPipSize = "1.0"
                                        newDigits = "1"
                                    } else {
                                        newPipSize = "1.0"
                                        newDigits = "2"
                                    }
                                },
                                label = { Text(cls, fontSize = 9.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmatTechCyan,
                                    selectedLabelColor = EmatTextWhite
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newPipSize,
                            onValueChange = { newPipSize = it },
                            label = { Text("Pip Size", fontSize = 10.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = EmatTextWhite,
                                unfocusedTextColor = EmatTextWhite,
                                focusedBorderColor = EmatTechCyan,
                                unfocusedBorderColor = EmatBorderHairline
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = newDigits,
                            onValueChange = { newDigits = it },
                            label = { Text("Digits", fontSize = 10.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = EmatTextWhite,
                                unfocusedTextColor = EmatTextWhite,
                                focusedBorderColor = EmatTechCyan,
                                unfocusedBorderColor = EmatBorderHairline
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sym = newSymbol.trim().uppercase()
                        val nm = if (newName.isBlank()) sym else newName.trim()
                        val pip = newPipSize.toDoubleOrNull() ?: 0.0001
                        val dig = newDigits.toIntOrNull() ?: 5
                        if (sym.isNotBlank() && onAddInstrument != null) {
                            onAddInstrument(sym, nm, newAssetClass, pip, dig)
                            newSymbol = ""
                            newName = ""
                            isAddInstrumentDialogOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmatTechCyan, contentColor = EmatTextWhite),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_add_asset_button")
                ) {
                    Text("REGISTER", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddInstrumentDialogOpen = false }) {
                    Text("CANCEL", color = EmatTextMuted, fontSize = 11.sp)
                }
            },
            containerColor = EmatSurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

