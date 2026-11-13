package com.example.emat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.emat.data.local.entity.InstrumentEntity
import com.example.emat.domain.model.MarketQuote
import com.example.emat.domain.service.MarketDataState

enum class InstrumentFeedStatus(val label: String) {
    LIVE("LIVE"),
    HISTORICAL("HISTORICAL"),
    UNAVAILABLE("UNAVAILABLE")
}

data class InstrumentDashboardItem(
    val symbol: String,
    val name: String,
    val assetClass: String,
    val status: InstrumentFeedStatus,
    val latestPrice: Double? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstrumentDashboardScreen(
    instruments: List<InstrumentEntity>,
    quotesBySymbol: Map<String, MarketQuote>,
    marketDataState: MarketDataState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboardItems = instruments.map { inst ->
        val quote = quotesBySymbol[inst.symbol]
        val status = when {
            marketDataState == MarketDataState.CONNECTED && quote != null -> InstrumentFeedStatus.LIVE
            quote != null -> InstrumentFeedStatus.HISTORICAL
            else -> InstrumentFeedStatus.UNAVAILABLE
        }
        val price = quote?.let { (it.bid + it.ask) / 2.0 }?.takeIf { !it.isNaN() }
        InstrumentDashboardItem(
            symbol = inst.symbol,
            name = inst.name,
            assetClass = inst.assetClass,
            status = status,
            latestPrice = price
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Registered Instruments (${dashboardItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = marketDataState.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("refresh_instruments_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh market quotes"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("instrument_dashboard_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dashboardItems, key = { it.symbol }) { item ->
                InstrumentDashboardRow(item = item)
            }
        }
    }
}

@Composable
private fun InstrumentDashboardRow(item: InstrumentDashboardItem) {
    val statusContainerColor = when (item.status) {
        InstrumentFeedStatus.LIVE -> MaterialTheme.colorScheme.primaryContainer
        InstrumentFeedStatus.HISTORICAL -> MaterialTheme.colorScheme.secondaryContainer
        InstrumentFeedStatus.UNAVAILABLE -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusContentColor = when (item.status) {
        InstrumentFeedStatus.LIVE -> MaterialTheme.colorScheme.onPrimaryContainer
        InstrumentFeedStatus.HISTORICAL -> MaterialTheme.colorScheme.onSecondaryContainer
        InstrumentFeedStatus.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("instrument_card_${item.symbol}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.symbol,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.name} • ${item.assetClass}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.latestPrice?.let { String.format(java.util.Locale.US, "%.5f", it) } ?: "--",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("instrument_price_${item.symbol}")
                )
                Surface(
                    color = statusContainerColor,
                    contentColor = statusContentColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = item.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .testTag("instrument_status_${item.symbol}")
                    )
                }
            }
        }
    }
}
