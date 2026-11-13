package com.example.emat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.example.emat.domain.model.EmatAccount
import com.example.emat.domain.model.EmatNavDestination
import com.example.emat.ui.theme.EmatBackground
import com.example.emat.ui.theme.EmatBorderHairline
import com.example.emat.ui.theme.EmatBorderSubtle
import com.example.emat.ui.theme.EmatFontFamily
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
fun EmatLeftDrawer(
    currentDestination: EmatNavDestination,
    currentAccount: EmatAccount,
    availableAccounts: List<EmatAccount>,
    onSelectDestination: (EmatNavDestination) -> Unit,
    onSwitchAccount: (EmatAccount) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(310.dp)
            .testTag("left_navigation_drawer"),
        color = EmatBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .padding(vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Branding & Platform Identity
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "AUTONOMIC PLATFORM",
                    color = EmatTechCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = EmatFontFamily
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EmatLongGreen)
                    )
                    Text(
                        text = "EMAT AUTONOMIQ",
                        color = EmatTextWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                }
                Text(
                    text = "Institutional Execution Intelligence",
                    color = EmatTextMuted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section 1: ACCOUNT
            DrawerSectionHeader(title = "ACCOUNT")

            // Active Account Card & Switcher
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmatSurfaceCard)
                    .border(1.dp, EmatBorderHairline, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentAccount.name,
                            color = EmatTextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (currentAccount.isLiveExecutionAllowed) Color(0x2210B981) else Color(0x226366F1))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (currentAccount.isLiveExecutionAllowed) "LIVE TIER 1" else "SUB-DESK",
                                color = if (currentAccount.isLiveExecutionAllowed) EmatLongGreen else Color(0xFFA5B4FC),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "${currentAccount.id} • ${currentAccount.role}",
                        color = EmatTextSubtle,
                        fontSize = 10.sp
                    )

                    HorizontalDivider(color = EmatBorderHairline, thickness = 1.dp)

                    // Quick Switch Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Switch Desk:",
                            color = EmatTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableAccounts.forEach { acc ->
                                val isCurrent = acc.id == currentAccount.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isCurrent) EmatTechCyan.copy(alpha = 0.2f) else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (isCurrent) EmatTechCyan else EmatBorderHairline,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onSwitchAccount(acc) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (acc.id.contains("INST")) "Master" else "Quant",
                                        color = if (isCurrent) EmatTechCyan else EmatTextSubtle,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            DrawerItem(
                icon = Icons.Default.Person,
                label = "Profile",
                isSelected = currentDestination == EmatNavDestination.PROFILE,
                onClick = { onSelectDestination(EmatNavDestination.PROFILE) }
            )

            // Section 2: CORE
            DrawerSectionHeader(title = "CORE")
            DrawerItem(
                icon = Icons.Default.Dashboard,
                label = "Dashboard",
                isSelected = currentDestination == EmatNavDestination.DASHBOARD,
                onClick = { onSelectDestination(EmatNavDestination.DASHBOARD) }
            )
            DrawerItem(
                icon = Icons.Default.Radar,
                label = "Market Intelligence",
                isSelected = currentDestination == EmatNavDestination.MARKET_INTELLIGENCE,
                onClick = { onSelectDestination(EmatNavDestination.MARKET_INTELLIGENCE) }
            )
            DrawerItem(
                icon = Icons.Default.Visibility,
                label = "Watchlist",
                isSelected = currentDestination == EmatNavDestination.WATCHLIST,
                onClick = { onSelectDestination(EmatNavDestination.WATCHLIST) }
            )
            DrawerItem(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                label = "Opportunities",
                isSelected = currentDestination == EmatNavDestination.OPPORTUNITIES,
                onClick = { onSelectDestination(EmatNavDestination.OPPORTUNITIES) }
            )
            DrawerItem(
                icon = Icons.Default.AccountBalanceWallet,
                label = "Trades",
                isSelected = currentDestination == EmatNavDestination.TRADES,
                onClick = { onSelectDestination(EmatNavDestination.TRADES) }
            )
            DrawerItem(
                icon = Icons.Default.Description,
                label = "Trade Journal",
                isSelected = currentDestination == EmatNavDestination.JOURNAL,
                onClick = { onSelectDestination(EmatNavDestination.JOURNAL) }
            )
            DrawerItem(
                icon = Icons.Default.FolderShared,
                label = "Evidence",
                isSelected = currentDestination == EmatNavDestination.EVIDENCE,
                onClick = { onSelectDestination(EmatNavDestination.EVIDENCE) }
            )

            // Section 3: ANALYTICS
            DrawerSectionHeader(title = "ANALYTICS")
            DrawerItem(
                icon = Icons.Default.QueryStats,
                label = "Performance",
                isSelected = currentDestination == EmatNavDestination.PERFORMANCE,
                onClick = { onSelectDestination(EmatNavDestination.PERFORMANCE) }
            )
            DrawerItem(
                icon = Icons.Default.Shield,
                label = "Risk Engine",
                isSelected = currentDestination == EmatNavDestination.RISK,
                onClick = { onSelectDestination(EmatNavDestination.RISK) }
            )
            DrawerItem(
                icon = Icons.Default.Description,
                label = "Reports",
                isSelected = currentDestination == EmatNavDestination.REPORTS,
                onClick = { onSelectDestination(EmatNavDestination.REPORTS) }
            )

            // Section 4: SYSTEM
            DrawerSectionHeader(title = "SYSTEM")
            DrawerItem(
                icon = Icons.Default.Notifications,
                label = "Notifications",
                isSelected = currentDestination == EmatNavDestination.NOTIFICATIONS,
                onClick = { onSelectDestination(EmatNavDestination.NOTIFICATIONS) }
            )
            DrawerItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = currentDestination == EmatNavDestination.SETTINGS,
                onClick = { onSelectDestination(EmatNavDestination.SETTINGS) }
            )
            DrawerItem(
                icon = Icons.Default.SwapHoriz,
                label = "My MT5 Broker",
                isSelected = currentDestination == EmatNavDestination.MY_MT5_BROKER,
                onClick = { onSelectDestination(EmatNavDestination.MY_MT5_BROKER) }
            )
            DrawerItem(
                icon = Icons.AutoMirrored.Filled.Help,
                label = "Help & Support",
                isSelected = currentDestination == EmatNavDestination.HELP_SUPPORT,
                onClick = { onSelectDestination(EmatNavDestination.HELP_SUPPORT) }
            )
            DrawerItem(
                icon = Icons.Default.Info,
                label = "Software Information",
                isSelected = currentDestination == EmatNavDestination.SOFTWARE_INFO,
                onClick = { onSelectDestination(EmatNavDestination.SOFTWARE_INFO) }
            )

            // Section 5: ACCOUNT CONTROL
            DrawerSectionHeader(title = "ACCOUNT CONTROL")
            DrawerItem(
                icon = Icons.Default.Security,
                label = "Security",
                isSelected = currentDestination == EmatNavDestination.SECURITY,
                onClick = { onSelectDestination(EmatNavDestination.SECURITY) }
            )
            DrawerItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                label = "Logout",
                isSelected = false,
                textColor = EmatShortRed,
                onClick = onLogout
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        color = EmatTextMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    textColor: Color = if (isSelected) EmatTechCyan else EmatTextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) EmatTechCyan.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) EmatTechCyan else textColor.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
