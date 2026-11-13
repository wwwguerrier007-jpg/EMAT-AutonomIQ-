package com.example.emat.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.emat.ui.theme.EmatFontFamily
import com.example.emat.ui.theme.EmatBackground
import com.example.emat.ui.theme.EmatBorderHairline
import com.example.emat.ui.theme.EmatBorderSubtle
import com.example.emat.ui.theme.EmatLongGreen
import com.example.emat.ui.theme.EmatPlatinumLight
import com.example.emat.ui.theme.EmatShortRed
import com.example.emat.ui.theme.EmatShortRedBorder
import com.example.emat.ui.theme.EmatShortRedDim
import com.example.emat.ui.theme.EmatSteelMuted
import com.example.emat.ui.theme.EmatSurface
import com.example.emat.ui.theme.EmatSurfaceActive
import com.example.emat.ui.theme.EmatTechCyan
import com.example.emat.ui.theme.EmatTechCyanGlow
import com.example.emat.ui.theme.EmatTextMuted
import com.example.emat.ui.theme.EmatTextOffset
import com.example.emat.ui.theme.EmatTextPrimary
import com.example.emat.ui.theme.EmatTextSubtle
import com.example.emat.ui.theme.EmatTextWhite
import com.example.emat.ui.theme.EmatWarningAmber
import com.example.emat.domain.service.MarketDataState
import com.example.emat.ui.theme.EmatWarningAmberBorder
import com.example.emat.ui.theme.EmatWarningAmberDim

@Composable
fun EmatTopBar(
    isOffline: Boolean,
    isEmergencyStopped: Boolean,
    sastTime: String,
    onEmergencyStopToggle: () -> Unit,
    onLogout: () -> Unit,
    onOpenLeftDrawer: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenRightDrawer: () -> Unit = {},
    marketDataState: MarketDataState = MarketDataState.NOT_CONNECTED,
    onToggleMarketDataState: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EmatBackground)
            .statusBarsPadding()
            .border(androidx.compose.foundation.BorderStroke(1.dp, EmatBorderHairline))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Header Row: Hamburger (Left ONLY) + Branding + Global Search + Context Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // One Hamburger Icon Rule: STRICTLY TOP-LEFT ONLY
                    IconButton(
                        onClick = onOpenLeftDrawer,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("topbar_hamburger_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Navigation Menu",
                            tint = EmatTextWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "AUTONOMIC INTELLIGENCE",
                            color = EmatTextSubtle,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(EmatTechCyan)
                                    .shadow(6.dp, CircleShape, spotColor = EmatTechCyanGlow)
                            )

                            Text(
                                text = "EMAT AUTONOMIQ",
                                color = EmatTextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.3).sp
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Global Search Icon Trigger
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("topbar_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Global Search",
                            tint = EmatTextWhite,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Context Actions Trigger Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmatTechCyan.copy(alpha = 0.15f))
                            .border(1.dp, EmatTechCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .clickable { onOpenRightDrawer() }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                            .testTag("topbar_actions_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Open Actions",
                                tint = EmatTechCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "ACTIONS",
                                color = EmatTechCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Quick Emergency Stop Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isEmergencyStopped) EmatShortRedDim else EmatSurfaceActive)
                            .border(
                                1.dp,
                                if (isEmergencyStopped) EmatShortRedBorder else EmatBorderSubtle,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onEmergencyStopToggle() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("topbar_emergency_stop_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEmergencyStopped) "HALTED" else "E-STOP",
                            color = if (isEmergencyStopped) EmatShortRed else EmatWarningAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Subtitle & SAST Clock
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 42.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Intelligence. Discipline. Evidence.",
                    color = EmatTextMuted,
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = sastTime,
                    color = EmatTextSubtle,
                    fontSize = 10.sp,
                    fontFamily = EmatFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Market Data Provider Status Banner
        if (marketDataState == MarketDataState.NOT_CONNECTED) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = EmatWarningAmberDim
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(EmatWarningAmber)
                        )
                        Text(
                            text = "MARKET DATA: NOT CONNECTED (Zero synthetic ticks)",
                            color = EmatWarningAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    if (onToggleMarketDataState != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(EmatWarningAmber.copy(alpha = 0.2f))
                                .border(1.dp, EmatWarningAmberBorder, RoundedCornerShape(4.dp))
                                .clickable { onToggleMarketDataState() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CONNECT FEED",
                                color = EmatWarningAmber,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        StatusBadge(status = "FAIL-SAFE")
                    }
                }
            }
        }

        // Offline Notification Banner if offline
        if (isOffline) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = EmatShortRedDim
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NO NETWORK CONNECTION — Market ticks flagged STALE.",
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    StatusBadge(status = "FAIL-SAFE")
                }
            }
        }
    }
}

