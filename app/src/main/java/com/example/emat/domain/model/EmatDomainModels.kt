package com.example.emat.domain.model

enum class AssetClass(val displayName: String) {
    FOREX("Forex"),
    METALS("Metals"),
    INDICES("Indices"),
    CRYPTO("Crypto")
}

enum class TradeDirection {
    BUY,
    SELL,
    NONE
}

enum class Timeframe(val label: String, val minutes: Int) {
    M5("M5", 5),
    M15("M15", 15),
    M30("M30", 30),
    H1("H1", 60),
    H4("H4", 240),
    D1("D1", 1440),
    W1("W1", 10080),
    MN("MN", 43200)
}

enum class MarketCondition(val label: String) {
    TRENDING_BULLISH("Trending Bullish"),
    TRENDING_BEARISH("Trending Bearish"),
    RANGING("Ranging Consolidation"),
    ACCUMULATION("Accumulation Phase"),
    EXPANSION("Momentum Expansion"),
    HIGH_VOLATILITY("High Volatility Spill")
}

enum class SetupStatus(val label: String) {
    QUALIFIED("Qualified"),
    MONITORING("Monitoring"),
    INVALIDATED("Invalidated"),
    NO_SETUP("No Setup")
}

enum class TimeframeAlignment(val label: String) {
    FULL_ALIGNMENT("H4 / H1 / M15 Confirmed"),
    PARTIAL_ALIGNMENT("H4 / H1 Aligned"),
    DIVERGENT("Divergent Timeframes"),
    UNCONFIRMED("Awaiting Confirmation")
}

enum class RiskStatus(val label: String) {
    APPROVED("Approved by Risk Engine"),
    REJECTED("Rejected: Limit / Exposure Exceeded"),
    AWAITING_REVIEW("Pending Risk Review"),
    BLOCKED_ONE_TRADE_RULE("Blocked: 1 Active Trade Limit")
}

enum class ComponentHealth(val label: String) {
    HEALTHY("HEALTHY"),
    WARNING("WARNING"),
    ERROR("ERROR"),
    NOT_CONNECTED("NOT CONNECTED"),
    AWAITING_CONFIGURATION("AWAITING CONFIGURATION")
}

enum class ExecutionState(val label: String) {
    NOT_CONNECTED("NOT CONNECTED"),
    READY("READY"),
    DEMO("DEMO MODE"),
    LIVE("LIVE EXECUTION"),
    ERROR("ERROR"),
    EMERGENCY_STOP("EMERGENCY STOPPED")
}

data class EceScoreBreakdown(
    val overall: Int,
    val trendScore: Int,
    val structureScore: Int,
    val momentumScore: Int,
    val confirmationScore: Int,
    val timeframeScore: Int,
    val setupQuality: Int,
    val riskScore: Int,
    val newsRiskScore: Int,
    val explanation: String
)

enum class DataOrigin(val label: String) {
    LIVE_FEED("LIVE FEED"),
    TEST_DATA("TEST DATA"),
    UNAVAILABLE("UNAVAILABLE")
}

data class MarketQuote(
    val symbol: String,
    val bid: Double,
    val ask: Double,
    val sourceTimestamp: Long,
    val receiveTimestamp: Long,
    val origin: DataOrigin = DataOrigin.LIVE_FEED,
    val lastPrice: Double = Double.NaN,
    val isSpreadAvailable: Boolean = !bid.isNaN() && !ask.isNaN()
)

data class OhlcvCandle(
    val symbol: String,
    val interval: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val timestamp: Long
)

data class Opportunity(
    val rank: Int,
    val symbol: String,
    val assetClass: AssetClass,
    val direction: TradeDirection,
    val eceScore: Int,
    val marketCondition: MarketCondition,
    val setupStatus: SetupStatus,
    val timeframeAlignment: TimeframeAlignment,
    val riskStatus: RiskStatus,
    val whyItQualifies: String,
    val timestamp: Long,
    val freshnessSeconds: Long,
    val isStale: Boolean,
    val dataOrigin: DataOrigin = DataOrigin.UNAVAILABLE,
    val spreadDisplay: String = "WATCH - SPREAD UNAVAILABLE",
    val isSpreadAvailable: Boolean = false
)

data class ActiveTrade(
    val tradeId: String,
    val symbol: String,
    val direction: TradeDirection,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val positionSizeLots: Double,
    val currentPrice: Double,
    val unrealizedPnl: Double,
    val isBreakEven: Boolean,
    val riskStatus: RiskStatus,
    val status: String,
    val executionMode: String,
    val openedAt: Long
)

data class NewsEvent(
    val id: String,
    val title: String,
    val currency: String,
    val impact: String, // "HIGH", "MEDIUM", "LOW"
    val scheduledSast: String, // South African Standard Time (SAST, UTC+2)
    val timeRemainingMinutes: Long,
    val isImminent: Boolean,
    val affectedInstruments: List<String>
)

enum class EmatNavDestination(val displayName: String) {
    DASHBOARD("Dashboard"),
    MARKET_INTELLIGENCE("Market Intelligence"),
    WATCHLIST("Watchlist"),
    OPPORTUNITIES("Opportunities"),
    TRADES("Trades"),
    JOURNAL("Trade Journal"),
    EVIDENCE("Evidence"),
    PERFORMANCE("Performance"),
    RISK("Risk Engine"),
    REPORTS("Reports"),
    NOTIFICATIONS("Notifications"),
    SETTINGS("Settings"),
    MY_MT5_BROKER("My MT5 Broker"),
    SECURITY("Security"),
    PROFILE("Operator Profile"),
    SOFTWARE_INFO("Software Information"),
    HELP_SUPPORT("Help & SOPs")
}

enum class AccountCategory(val code: String, val displayName: String) {
    INSTITUTIONAL("INST", "Institutional Organization"),
    INDIVIDUAL("IND", "Individual Trader");

    companion object {
        fun fromCode(code: String?): AccountCategory {
            if (code == null) return INDIVIDUAL
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) || it.name.equals(code.trim(), ignoreCase = true) } ?: INDIVIDUAL
        }
    }
}

data class EmatAccount(
    val id: String,
    val name: String,
    val role: String,
    val tier: String,
    val dailyLossLimit: String,
    val maxPositionSize: String,
    val isLiveExecutionAllowed: Boolean,
    val permissions: List<String>,
    val category: AccountCategory = AccountCategory.INDIVIDUAL
)

data class EvidenceRecord(
    val evidenceId: String,
    val instrument: String,
    val timeframe: String,
    val sourceRef: String,
    val timestamp: Long,
    val observation: String,
    val calculation: String,
    val interpretation: String,
    val relatedOpportunityId: String? = null,
    val relatedTradeId: String? = null,
    val verificationStatus: String = "VERIFIED_AUDIT"
)

data class PerformanceMetrics(
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRatePercent: Double,
    val averageWinPips: Double,
    val averageLossPips: Double,
    val profitFactor: Double,
    val netPnL: Double,
    val maxDrawdownPercent: Double,
    val activeTradesCount: Int,
    val isStatisticallySignificant: Boolean,
    val notes: String
)

data class SearchResultItem(
    val category: String, // "INSTRUMENT", "OPPORTUNITY", "TRADE", "EVIDENCE", "REPORT", "NOTIFICATION"
    val title: String,
    val subtitle: String,
    val status: String,
    val destination: EmatNavDestination,
    val targetSymbol: String? = null
)

data class AiAnalysisResult(
    val contextTitle: String,
    val fact: String,
    val calculation: String,
    val analysis: String,
    val inference: String,
    val recommendation: String,
    val timestamp: String,
    val dataTruthStatus: String // "LIVE_VERIFIED", "CALCULATED", "HISTORICAL"
)

const val DEFAULT_TRIAL_DURATION_DAYS: Int = 14

enum class EntitlementState(val label: String) {
    NOT_SUBSCRIBED("NOT_SUBSCRIBED"),
    TRIAL_ACTIVE("TRIAL_ACTIVE"),
    TRIAL_EXPIRED("TRIAL_EXPIRED"),
    SUBSCRIPTION_ACTIVE("SUBSCRIPTION_ACTIVE"),
    SUBSCRIPTION_EXPIRED("SUBSCRIPTION_EXPIRED")
}

data class AccountEntitlement(
    val accountId: String,
    val state: EntitlementState,
    val trialStartedAt: Long? = null,
    val trialDurationDays: Int = DEFAULT_TRIAL_DURATION_DAYS,
    val accumulatedTrialDurationMillis: Long = 0L,
    val lastObservedTimestamp: Long = 0L,
    val hasClockAnomaly: Boolean = false,
    val subscriptionId: String? = null,
    val subscriptionPlanId: String? = null,
    val subscriptionPurchasedAt: Long? = null,
    val subscriptionExpiresAt: Long? = null,
    val verifiedByBillingProvider: Boolean = false,
    val remainingTrialMillis: Long = 0L
) {
    val planId: String? get() = subscriptionPlanId
    val remainingTrialDays: Int get() = kotlin.math.ceil(remainingTrialMillis.toDouble() / (24 * 3600 * 1000.0)).toInt().coerceAtLeast(0)
    val remainingTrialHours: Int get() = kotlin.math.ceil(remainingTrialMillis.toDouble() / (3600 * 1000.0)).toInt().coerceAtLeast(0)
}

enum class BridgeStatus(val label: String) {
    NOT_CONFIGURED("BRIDGE NOT CONFIGURED"),
    CONFIGURED("BRIDGE CONFIGURED"),
    CONNECTED("BRIDGE CONNECTED")
}

enum class BrokerAuthStatus(val label: String) {
    UNAUTHENTICATED("UNAUTHENTICATED"),
    AUTHENTICATING("AUTHENTICATING (READ-ONLY)..."),
    AUTHENTICATED("AUTHENTICATED (READ-ONLY)"),
    FAILED("AUTHENTICATION FAILED")
}

enum class MarketDataFeedStatus(val label: String) {
    STANDBY("STANDBY"),
    STREAMING("STREAMING"),
    VALIDATED("VALIDATED (19/19)")
}

data class Mt5BrokerConfig(
    val brokerName: String = "XM Global",
    val brokerServer: String = "XMGlobal-MT5 10",
    val accountNumber: String = "346533676",
    val accountType: String = "DEMO",
    val investorPasswordMasked: Boolean = true,
    val bridgeUrl: String = "",
    val bridgeTokenMasked: Boolean = false,
    val isConfigured: Boolean = false,
    val connectionState: ComponentHealth = ComponentHealth.NOT_CONNECTED,
    val lastSyncTime: String = "--",
    val statusMessage: String = "BRIDGE NOT CONFIGURED",
    val bridgeStatus: BridgeStatus = BridgeStatus.NOT_CONFIGURED,
    val brokerAuthStatus: BrokerAuthStatus = BrokerAuthStatus.UNAUTHENTICATED,
    val marketDataStatus: MarketDataFeedStatus = MarketDataFeedStatus.STANDBY,
    val lastSuccessfulConnection: String = "--",
    val lastReceivedTick: String = "--",
    val availableInstrumentsCount: Int = 0,
    val currentWorkflowStep: Int = 0,
    val workflowStepDescription: String = ""
)

data class Mt5InstrumentComparison(
    val ematSymbol: String,
    val brokerSymbol: String = "--",
    val brokerBid: Double? = null,
    val brokerAsk: Double? = null,
    val brokerSpread: Double? = null,
    val biquoteBid: Double? = null,
    val biquoteAsk: Double? = null,
    val priceDiff: Double? = null,
    val lastTimestamp: String = "--",
    val freshness: String = "FRESH",
    val sourceStatus: String = "STANDBY"
)

