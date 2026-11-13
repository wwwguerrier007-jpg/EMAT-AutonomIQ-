package com.example.emat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.emat.domain.model.AiAnalysisResult
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
fun EmatAiContextDialog(
    isOpen: Boolean,
    analysisResult: AiAnalysisResult?,
    onClose: () -> Unit
) {
    if (!isOpen || analysisResult == null) return

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("ai_context_dialog"),
            color = EmatBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close AI Intelligence",
                                tint = EmatTextWhite
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = EmatTechCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "EMAT AI DECISION SUPPORT",
                                    color = EmatTechCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    fontFamily = EmatFontFamily
                                )
                            }
                            Text(
                                text = analysisResult.contextTitle,
                                color = EmatTextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Truth Status Badge
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmatLongGreen.copy(alpha = 0.15f))
                            .border(1.dp, EmatLongGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = analysisResult.dataTruthStatus,
                            color = EmatLongGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = EmatBorderHairline, thickness = 1.dp)

                // Body content: Strict 5-Section Intelligence Breakdown
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. FACT Section
                    IntelligenceBlock(
                        title = "1. FACT (VERIFIABLE OBSERVATION)",
                        content = analysisResult.fact,
                        icon = Icons.Default.FactCheck,
                        accentColor = EmatTechCyan,
                        tag = "RAW_TELEMETRY"
                    )

                    // 2. CALCULATION Section
                    IntelligenceBlock(
                        title = "2. CALCULATION (MATHEMATICAL MODEL)",
                        content = analysisResult.calculation,
                        icon = Icons.Default.Calculate,
                        accentColor = Color(0xFF60A5FA),
                        tag = "QUANT_FORMULA"
                    )

                    // 3. ANALYSIS Section
                    IntelligenceBlock(
                        title = "3. ANALYSIS (STRUCTURAL INTERPRETATION)",
                        content = analysisResult.analysis,
                        icon = Icons.Default.Insights,
                        accentColor = Color(0xFFA78BFA),
                        tag = "CORE_ECE"
                    )

                    // 4. INFERENCE Section
                    IntelligenceBlock(
                        title = "4. INFERENCE (PROBABILISTIC TRAJECTORY)",
                        content = analysisResult.inference,
                        icon = Icons.Default.Psychology,
                        accentColor = EmatWarningAmber,
                        tag = "STATISTICAL"
                    )

                    // 5. RECOMMENDATION Section
                    IntelligenceBlock(
                        title = "5. RECOMMENDATION (OPERATOR DECISION SUPPORT)",
                        content = analysisResult.recommendation,
                        icon = Icons.Default.Recommend,
                        accentColor = EmatLongGreen,
                        tag = "ACTIONABLE_SOP"
                    )

                    // Institutional Compliance Footnote
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmatSurfaceCard)
                            .border(1.dp, EmatBorderHairline, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = EmatTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "INSTITUTIONAL GOVERNANCE NOTICE",
                                    color = EmatTextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "EMAT AI provides non-discretionary decision-support based on audited statistical criteria. It does not promise guaranteed profits or bypass risk limits. All executions require human operator authorization and strictly comply with the One-Trade Rule invariant.",
                                color = EmatTextSubtle,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun IntelligenceBlock(
    title: String,
    content: String,
    icon: ImageVector,
    accentColor: Color,
    tag: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontFamily = EmatFontFamily
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tag,
                        color = accentColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = content,
                color = EmatTextPrimary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
