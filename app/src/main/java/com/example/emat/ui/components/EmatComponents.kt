package com.example.emat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emat.domain.model.Opportunity
import com.example.emat.domain.model.TradeDirection
import com.example.emat.ui.theme.EmatFontFamily
import com.example.emat.ui.theme.ElectricBlueBorder
import com.example.emat.ui.theme.ElectricBlueBright
import com.example.emat.ui.theme.ElectricBlueDim
import com.example.emat.ui.theme.EmatBackground
import com.example.emat.ui.theme.EmatBearishRed
import com.example.emat.ui.theme.EmatBorderHairline
import com.example.emat.ui.theme.EmatBorderMedium
import com.example.emat.ui.theme.EmatBorderSubtle
import com.example.emat.ui.theme.EmatBullishGreen
import com.example.emat.ui.theme.EmatLongGreen
import com.example.emat.ui.theme.EmatLongGreenBorder
import com.example.emat.ui.theme.EmatLongGreenDim
import com.example.emat.ui.theme.EmatPlatinumLight
import com.example.emat.ui.theme.EmatShortRed
import com.example.emat.ui.theme.EmatShortRedBorder
import com.example.emat.ui.theme.EmatShortRedDim
import com.example.emat.ui.theme.EmatSilverPrimary
import com.example.emat.ui.theme.EmatSilverSecondary
import com.example.emat.ui.theme.EmatSteelDark
import com.example.emat.ui.theme.EmatSteelMuted
import com.example.emat.ui.theme.EmatSurface
import com.example.emat.ui.theme.EmatSurfaceActive
import com.example.emat.ui.theme.EmatSurfaceCard
import com.example.emat.ui.theme.EmatSurfaceElevated
import com.example.emat.ui.theme.EmatTechCyan
import com.example.emat.ui.theme.EmatTechCyanBorder
import com.example.emat.ui.theme.EmatTechCyanDim
import com.example.emat.ui.theme.EmatTextMuted
import com.example.emat.ui.theme.EmatTextOffset
import com.example.emat.ui.theme.EmatTextPrimary
import com.example.emat.ui.theme.EmatTextSubtle
import com.example.emat.ui.theme.EmatTextWhite
import com.example.emat.ui.theme.EmatWarningAmber
import com.example.emat.ui.theme.EmatWarningAmberBorder
import com.example.emat.ui.theme.EmatWarningAmberDim
import com.example.emat.ui.theme.SleekDeepSlateBorderHairline
import com.example.emat.ui.theme.SleekDeepSlateMutedText
import com.example.emat.ui.theme.SleekDeepSlateText
import com.example.emat.ui.viewmodel.MainTab

@Composable
fun MetallicCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = EmatSurfaceCard,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, EmatBorderHairline, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        content()
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor, dotColor) = when (status.uppercase()) {
        "REAL", "HEALTHY", "APPROVED", "QUALIFIED", "LIVE", "SYSTEM HEALTHY" -> 
            Quadruple(EmatLongGreenDim, EmatLongGreen, EmatLongGreenBorder, EmatLongGreen)
        "TEST", "TEST MODE", "SCANNING", "ACTIVE", "ONLINE", "VERIFIED", "STANDBY", "ARMED", "RUNNING", "OPTIMAL" -> 
            Quadruple(ElectricBlueDim, ElectricBlueBright, ElectricBlueBorder, ElectricBlueBright)
        "DEMO", "DEMO MODE" -> 
            Quadruple(Color(0x1A6366F1), Color(0xFFA5B4FC), Color(0x336366F1), Color(0xFF6366F1))
        "WARNING", "STALE", "MONITORING", "CONFIGURING" -> 
            Quadruple(EmatWarningAmberDim, EmatWarningAmber, EmatWarningAmberBorder, EmatWarningAmber)
        "ERROR", "REJECTED", "EMERGENCY_STOP", "EMERGENCY STOPPED", "HALTED", "OFFLINE" -> 
            Quadruple(EmatShortRedDim, EmatShortRed, EmatShortRedBorder, EmatShortRed)
        "NOT CONNECTED", "NOT_CONNECTED", "AWAITING CONFIGURATION" -> 
            Quadruple(Color(0x1A475569), SleekDeepSlateText, SleekDeepSlateBorderHairline, SleekDeepSlateMutedText)
        else -> 
            Quadruple(Color(0x1A334155), SleekDeepSlateText, SleekDeepSlateBorderHairline, SleekDeepSlateMutedText)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = status.uppercase(),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun EmatBottomBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, EmatBorderSubtle)),
        color = EmatSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                Triple(MainTab.HOME, "HOME", Icons.Default.Home),
                Triple(MainTab.SCANNER, "SCANNER", Icons.Default.QueryStats),
                Triple(MainTab.TRADES, "TRADES", Icons.Default.AccountBalanceWallet),
                Triple(MainTab.JOURNAL, "JOURNAL", Icons.Default.Shield),
                Triple(MainTab.INTELLIGENCE, "INTEL", Icons.Default.Psychology)
            )

            items.forEach { (tab, label, icon) ->
                val selected = currentTab == tab
                val itemColor = if (selected) ElectricBlueBright else EmatTextMuted

                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("nav_tab_${label.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = itemColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = label,
                        color = itemColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OpportunityCard(
    opportunity: Opportunity,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isNone = opportunity.symbol == "NONE"
    val accentColor = when {
        isNone -> Color(0x33FFFFFF)
        opportunity.direction == TradeDirection.BUY -> EmatLongGreen
        else -> EmatShortRed
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isNone) { onClick() }
            .testTag("opportunity_card_rank_${opportunity.rank}"),
        colors = CardDefaults.cardColors(containerColor = EmatSurfaceElevated),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Sleek Left Accent Bar (border-l-4)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (isNone) 80.dp else 128.dp)
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                if (isNone) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "0${opportunity.rank}",
                                color = EmatTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "AWAITING...",
                                color = EmatTextMuted,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = EmatFontFamily
                            )
                        }
                        StatusBadge(status = "SCANNING M15/H1")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Evaluating Registry — No qualifying setup satisfies EMAT rules.",
                        color = EmatTextOffset,
                        fontSize = 11.sp
                    )
                } else {
                    // Top Row: Number, Symbol, Direction badge, ECE Score
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "0${opportunity.rank}",
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = opportunity.symbol,
                                    color = EmatTextWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = EmatFontFamily,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val isBuy = opportunity.direction == TradeDirection.BUY
                                val dirBg = if (isBuy) EmatLongGreenDim else EmatShortRedDim
                                val dirColor = if (isBuy) EmatLongGreen else EmatShortRed
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(dirBg)
                                        .border(1.dp, if (isBuy) EmatLongGreenBorder else EmatShortRedBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = opportunity.direction.name,
                                        color = dirColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text("•", color = EmatTextMuted, fontSize = 10.sp)
                                Text(
                                    text = opportunity.timeframeAlignment.label,
                                    color = EmatTextSubtle,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Right Side: Large ECE Score + confidence rating
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${opportunity.eceScore}",
                                    color = EmatTextWhite,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = (-1).sp
                                )
                                Text(
                                    text = " ECE",
                                    color = EmatTextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                            Text(
                                text = if (opportunity.eceScore >= 80) "HIGH CONFIDENCE" else "MODERATE RISK",
                                color = EmatTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Rationale Box with sleek background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmatSurfaceCard)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = opportunity.whyItQualifies,
                                color = EmatTextSubtle,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${opportunity.freshnessSeconds}s ago",
                                color = EmatTextOffset,
                                fontSize = 10.sp,
                                fontFamily = EmatFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

