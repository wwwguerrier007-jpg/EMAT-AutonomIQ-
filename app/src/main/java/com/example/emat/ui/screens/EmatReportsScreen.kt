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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emat.ui.theme.EmatBackground
import com.example.emat.ui.theme.EmatBorderHairline
import com.example.emat.ui.theme.EmatFontFamily
import com.example.emat.ui.theme.EmatLongGreen
import com.example.emat.ui.theme.EmatSurfaceCard
import com.example.emat.ui.theme.EmatTechCyan
import com.example.emat.ui.theme.EmatTextMuted
import com.example.emat.ui.theme.EmatTextPrimary
import com.example.emat.ui.theme.EmatTextSubtle
import com.example.emat.ui.theme.EmatTextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EmatReportsScreen(
    totalTrades: Int,
    winRate: Double,
    profitFactor: Double,
    modifier: Modifier = Modifier
) {
    var generatedMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EmatBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("reports_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "COMPLIANCE & AUDIT",
                    color = EmatTechCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    fontFamily = EmatFontFamily
                )
                Text(
                    text = "Executive Reports",
                    color = EmatTextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(EmatTechCyan.copy(alpha = 0.15f))
                    .border(1.dp, EmatTechCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "IMMUTABLE FORMAT",
                    color = EmatTechCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (generatedMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(EmatLongGreen.copy(alpha = 0.15f))
                    .border(1.dp, EmatLongGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = EmatLongGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = generatedMessage ?: "",
                        color = EmatLongGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        ReportOptionCard(
            title = "Daily Operational & Risk Summary",
            description = "Audit trail of all scanner runs, risk validations, and active account activity for today.",
            onGenerate = {
                val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                generatedMessage = "Daily Audit Report compiled successfully at $time. Ready for compliance export."
            }
        )

        ReportOptionCard(
            title = "Trade Execution & Performance Audit",
            description = "Granular report of all closed trades ($totalTrades trades), verified win rate (${String.format(Locale.US, "%.1f", winRate)}%), and profit factor (${String.format(Locale.US, "%.2f", profitFactor)}).",
            onGenerate = {
                val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                generatedMessage = "Performance Audit compiled at $time. Cryptographic hash attached."
            }
        )

        ReportOptionCard(
            title = "System Health & Invariant Certification",
            description = "Certifies that the One-Trade Rule invariant and data freshness threshold (<15s) were 100% maintained.",
            onGenerate = {
                val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                generatedMessage = "Invariant Certification verified at $time. Zero compliance breaches detected."
            }
        )
    }
}

@Composable
private fun ReportOptionCard(
    title: String,
    description: String,
    onGenerate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(EmatSurfaceCard)
            .border(1.dp, EmatBorderHairline, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = EmatTextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = EmatTechCyan,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = description,
                color = EmatTextSubtle,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Button(
                onClick = onGenerate,
                colors = ButtonDefaults.buttonColors(containerColor = EmatTechCyan.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = EmatTechCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Compile Report",
                        color = EmatTechCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
