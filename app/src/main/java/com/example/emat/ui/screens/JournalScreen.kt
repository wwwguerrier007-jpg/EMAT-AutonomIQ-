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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emat.ui.theme.EmatFontFamily
import com.example.emat.data.local.entity.AuditLogEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JournalScreen(
    auditLogs: List<AuditLogEntity>,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(auditLogs, selectedCategory) {
        if (selectedCategory == "ALL") auditLogs
        else auditLogs.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(EmatBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("journal_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Journal Header
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
                        text = "AUDIT TRAIL & EVIDENCE REPOSITORY",
                        color = EmatTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Immutable System Journal",
                        color = EmatTextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Every scan, risk check, rule evaluation, and user decision is permanently logged with complete rationale and state.",
                        color = EmatTextSubtle,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Filter Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val categories = listOf("ALL", "SCANNER", "RISK", "EXECUTION", "EMERGENCY", "AUTH", "SYSTEM")
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmatTechCyan,
                                    selectedLabelColor = EmatTextWhite,
                                    containerColor = EmatSurfaceActive,
                                    labelColor = EmatTextMuted
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedCategory == cat,
                                    borderColor = if (selectedCategory == cat) EmatTechCyan else EmatBorderHairline
                                ),
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                }
            }
        }

        // Total Log Count summary
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECORDED EVENTS (${filteredLogs.size})",
                    color = EmatTextSubtle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "LATEST FIRST",
                    color = EmatTechCyan,
                    fontSize = 10.sp,
                    fontFamily = EmatFontFamily
                )
            }
        }

        // Empty state
        if (filteredLogs.isEmpty()) {
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
                        Text("No audit log events found for category $selectedCategory.", color = EmatTextSubtle, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(filteredLogs) { log ->
                AuditLogItem(log = log)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AuditLogItem(
    log: AuditLogEntity,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val formattedDate = remember(log.timestamp) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        sdf.format(Date(log.timestamp))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, EmatBorderHairline, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .testTag("audit_log_item_${log.id}"),
        colors = CardDefaults.cardColors(containerColor = EmatSurfaceCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Action Name, Category, Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.action,
                        color = EmatTextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formattedDate,
                        color = EmatTextMuted,
                        fontSize = 10.sp,
                        fontFamily = EmatFontFamily
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = log.category)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = EmatTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actor and Decision Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Actor: ${log.actor}",
                    color = EmatTechCyan,
                    fontSize = 11.sp,
                    fontFamily = EmatFontFamily
                )
                Text(
                    text = "Decision: ${log.decisionMade}",
                    color = EmatTextSubtle,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            // Expanded Full Institutional Evidence View
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = EmatSurfaceActive
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EvidenceField("WHAT HAPPENED?", log.action)
                        EvidenceField("WHEN?", formattedDate)
                        EvidenceField("WHY DID IT HAPPEN?", log.rationale)
                        EvidenceField("WHAT DATA WAS AVAILABLE?", log.dataAvailable)
                        EvidenceField("WHAT DECISION WAS MADE?", log.decisionMade)
                        EvidenceField("WHAT HAPPENED AFTERWARDS?", log.outcome)
                    }
                }
            }
        }
    }
}

@Composable
private fun EvidenceField(title: String, content: String) {
    Column {
        Text(
            text = title,
            color = EmatTextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = content,
            color = EmatTextWhite,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

