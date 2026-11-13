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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.emat.domain.model.EmatNavDestination
import com.example.emat.domain.model.SearchResultItem
import com.example.emat.ui.theme.EmatBackground
import com.example.emat.ui.theme.EmatBorderHairline
import com.example.emat.ui.theme.EmatFontFamily
import com.example.emat.ui.theme.EmatLongGreen
import com.example.emat.ui.theme.EmatShortRed
import com.example.emat.ui.theme.EmatSurfaceCard
import com.example.emat.ui.theme.EmatSurfaceElevated
import com.example.emat.ui.theme.EmatTechCyan
import com.example.emat.ui.theme.EmatTextMuted
import com.example.emat.ui.theme.EmatTextPrimary
import com.example.emat.ui.theme.EmatTextSubtle
import com.example.emat.ui.theme.EmatTextWhite
import com.example.emat.ui.theme.EmatWarningAmber

@Composable
fun EmatGlobalSearchDialog(
    isOpen: Boolean,
    query: String,
    results: List<SearchResultItem>,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onSelectResult: (SearchResultItem) -> Unit
) {
    if (!isOpen) return

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            focusRequester.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("global_search_dialog"),
            color = EmatBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Search Input Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EmatTextWhite
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmatSurfaceElevated)
                            .border(1.dp, EmatBorderHairline, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = EmatTechCyan,
                                modifier = Modifier.size(18.dp)
                            )

                            Box(modifier = Modifier.weight(1f)) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search instruments, setups, trades, evidence...",
                                        color = EmatTextMuted,
                                        fontSize = 13.sp
                                    )
                                }
                                BasicTextField(
                                    value = query,
                                    onValueChange = onQueryChange,
                                    textStyle = TextStyle(
                                        color = EmatTextWhite,
                                        fontSize = 13.sp,
                                        fontFamily = EmatFontFamily
                                    ),
                                    cursorBrush = SolidColor(EmatTechCyan),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .testTag("global_search_input")
                                )
                            }

                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { onQueryChange("") },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = EmatTextSubtle,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = EmatBorderHairline, thickness = 1.dp)

                // Results list or suggestions
                if (results.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = EmatTextMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No matching records found",
                                color = EmatTextSubtle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Try searching for 'XAUUSD', 'BTCUSD', 'OPPORTUNITY', 'TRADE', or 'ECE'",
                                color = EmatTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(results) { item ->
                            SearchResultRow(
                                item = item,
                                onClick = { onSelectResult(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    item: SearchResultItem,
    onClick: () -> Unit
) {
    val categoryColor = when (item.category) {
        "INSTRUMENT" -> EmatTechCyan
        "OPPORTUNITY" -> EmatLongGreen
        "TRADE" -> EmatWarningAmber
        "EVIDENCE" -> Color(0xFFA78BFA)
        "REPORT" -> Color(0xFF60A5FA)
        else -> EmatTextSubtle
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(EmatSurfaceCard)
            .border(1.dp, EmatBorderHairline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("search_result_${item.title.take(10).replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(categoryColor.copy(alpha = 0.15f))
                            .border(0.5.dp, categoryColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.category,
                            color = categoryColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = item.title,
                        color = EmatTextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = item.subtitle,
                    color = EmatTextSubtle,
                    fontSize = 11.sp
                )
            }

            if (item.status != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(EmatSurfaceElevated)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.status,
                        color = EmatTextPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
