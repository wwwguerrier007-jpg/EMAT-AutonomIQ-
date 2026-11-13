package com.example.emat.data.repository

import com.example.emat.data.local.EmatDatabase
import com.example.emat.data.local.entity.AuditLogEntity
import com.example.emat.data.local.entity.EceScoreEntity
import com.example.emat.data.local.entity.EvidenceEntity
import com.example.emat.data.local.entity.InstrumentEntity
import com.example.emat.data.local.entity.MarketDataEntity
import com.example.emat.data.local.entity.OpportunityEntity
import com.example.emat.data.local.entity.RiskCheckEntity
import com.example.emat.data.local.entity.ScannerRunEntity
import com.example.emat.data.local.entity.SystemHealthEntity
import com.example.emat.data.local.entity.TradeEntity
import com.example.emat.domain.model.ActiveTrade
import com.example.emat.domain.model.AssetClass
import com.example.emat.domain.model.ComponentHealth
import com.example.emat.domain.model.DataOrigin
import com.example.emat.domain.model.EvidenceRecord
import com.example.emat.domain.model.MarketCondition
import com.example.emat.domain.model.Opportunity
import com.example.emat.domain.model.RiskStatus
import com.example.emat.domain.model.SetupStatus
import com.example.emat.domain.model.TimeframeAlignment
import com.example.emat.domain.model.TradeDirection
import com.example.emat.domain.service.DefaultMarketDataProvider
import com.example.emat.domain.service.EceEngine
import com.example.emat.domain.service.ExecutionEngine
import com.example.emat.domain.service.MarketDataProvider
import com.example.emat.domain.service.MarketDataState
import com.example.emat.domain.service.NewsIntelligenceService
import com.example.emat.domain.service.RiskEngine
import com.example.emat.domain.service.StrategyEngine
import com.example.emat.data.local.entity.AccountEntitlementEntity
import com.example.emat.domain.model.AccountEntitlement
import com.example.emat.domain.model.EntitlementState
import com.example.emat.domain.model.DEFAULT_TRIAL_DURATION_DAYS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

interface EmatRepository {
    fun getInstrumentsFlow(): Flow<List<InstrumentEntity>>
    suspend fun addInstrument(instrument: InstrumentEntity, accountId: String = "EMAT-INST-001"): Boolean
    suspend fun removeInstrument(symbol: String)
    suspend fun updateInstrument(instrument: InstrumentEntity)
    suspend fun toggleInstrument(symbol: String, isActive: Boolean, accountId: String = "EMAT-INST-001"): Boolean
    suspend fun generateAiAnalysis(
        accountId: String,
        contextTitle: String,
        activeTrades: Int,
        isOffline: Boolean,
        selectedOpportunity: Opportunity? = null
    ): com.example.emat.domain.model.AiAnalysisResult?

    fun getTopOpportunitiesFlow(): Flow<List<Opportunity>>
    fun getActiveTradeFlow(): Flow<ActiveTrade?>
    fun getActiveTradeByAccountFlow(accountId: String): Flow<ActiveTrade?>

    fun getAuditLogsFlow(): Flow<List<AuditLogEntity>>
    fun getAuditLogsByAccountFlow(accountId: String): Flow<List<AuditLogEntity>>

    fun getEvidenceByAccountFlow(accountId: String): Flow<List<EvidenceRecord>>
    fun getSystemHealthFlow(): Flow<List<SystemHealthEntity>>

    suspend fun executeMarketScan(dataProviderType: String = "TEST", accountId: String = "EMAT-INST-001"): List<Opportunity>
    suspend fun logAuditEvent(
        accountId: String,
        category: String,
        action: String,
        actor: String,
        rationale: String,
        dataAvailable: String,
        decisionMade: String,
        outcome: String,
        eventMetadata: String = "{}"
    )
    suspend fun logEvidence(record: EvidenceRecord, accountId: String)

    suspend fun runDatabaseIntegrityCheck(): DbIntegrityResult
    suspend fun runAiAnalyzerSelfCheck(): Boolean
    suspend fun triggerEmergencyStop()
    suspend fun resetEmergencyStop()
    suspend fun updateSystemHealth(component: String, status: ComponentHealth, details: String)
    suspend fun isOneTradeRuleViolated(accountId: String): Boolean
    suspend fun submitTradeSafely(trade: TradeEntity): Boolean

    fun getAllTradesFlow(): Flow<List<TradeEntity>>
    fun getTradesByAccountFlow(accountId: String): Flow<List<TradeEntity>>

    fun getAccountEntitlementFlow(accountId: String): Flow<AccountEntitlement>
    suspend fun getAccountEntitlement(accountId: String, now: Long = System.currentTimeMillis()): AccountEntitlement
    suspend fun startTrialSafely(accountId: String): Boolean
    suspend fun refreshEntitlementObservation(accountId: String, now: Long = System.currentTimeMillis()): AccountEntitlement
}

data class DbIntegrityResult(
    val status: String,
    val isOk: Boolean,
    val version: Int,
    val timestamp: Long,
    val details: String
)

class EmatRepositoryImpl(
    private val db: EmatDatabase,
    private val strategyEngine: StrategyEngine = StrategyEngine(),
    private val eceEngine: EceEngine = EceEngine(),
    private val riskEngine: RiskEngine = RiskEngine(),
    val executionEngine: ExecutionEngine = ExecutionEngine(),
    private val newsService: NewsIntelligenceService = NewsIntelligenceService(),
    private val marketDataProvider: MarketDataProvider = DefaultMarketDataProvider()
) : EmatRepository {

    override fun getAllTradesFlow(): Flow<List<TradeEntity>> =
        db.tradeDao().getAllTrades()

    override fun getTradesByAccountFlow(accountId: String): Flow<List<TradeEntity>> =
        db.tradeDao().getTradesByAccountFlow(accountId)

    override fun getInstrumentsFlow(): Flow<List<InstrumentEntity>> =
        db.instrumentDao().getAllInstruments()

    override suspend fun addInstrument(instrument: InstrumentEntity, accountId: String): Boolean {
        val currentEntitlement = getAccountEntitlement(accountId)
        if (com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(
                com.example.emat.domain.service.EmatFeature.INSTRUMENT_REGISTRY_MUTATION,
                currentEntitlement.state
            )
        ) {
            logAuditEvent(
                accountId = accountId,
                category = "ENTITLEMENT",
                action = "FEATURE_ACCESS_DENIED",
                actor = "REPOSITORY",
                rationale = "Instrument add blocked: account entitlement state ${currentEntitlement.state.name} does not permit registry mutation",
                dataAvailable = "Feature: INSTRUMENT_REGISTRY_MUTATION, State: ${currentEntitlement.state.name}",
                decisionMade = "Reject instrument add at repository boundary",
                outcome = "INSTRUMENT_GATED"
            )
            return false
        }
        db.instrumentDao().insertInstrument(instrument)
        logAuditEvent(
            accountId = "SYSTEM",
            category = "SYSTEM",
            action = "REGISTRY_ADD_INSTRUMENT",
            actor = "OPERATOR",
            rationale = "Operator added new instrument to universe registry",
            dataAvailable = "Symbol: ${instrument.symbol}, Name: ${instrument.name}, Class: ${instrument.assetClass}",
            decisionMade = "Inserted into active database registry",
            outcome = "Universe registry expanded"
        )
        return true
    }

    override suspend fun removeInstrument(symbol: String) {
        db.instrumentDao().deleteInstrument(symbol)
        logAuditEvent(
            accountId = "SYSTEM",
            category = "SYSTEM",
            action = "REGISTRY_REMOVE_INSTRUMENT",
            actor = "OPERATOR",
            rationale = "Operator removed instrument from registry",
            dataAvailable = "Symbol: $symbol",
            decisionMade = "Deleted from active database registry",
            outcome = "Universe registry reduced"
        )
    }

    override suspend fun updateInstrument(instrument: InstrumentEntity) {
        db.instrumentDao().updateInstrument(instrument)
        logAuditEvent(
            accountId = "SYSTEM",
            category = "SYSTEM",
            action = "REGISTRY_UPDATE_INSTRUMENT",
            actor = "OPERATOR",
            rationale = "Operator modified instrument parameters",
            dataAvailable = "Symbol: ${instrument.symbol}, PipSize: ${instrument.pipSize}, Digits: ${instrument.digits}",
            decisionMade = "Updated metadata in database registry",
            outcome = "Registry synchronized"
        )
    }

    override suspend fun toggleInstrument(symbol: String, isActive: Boolean, accountId: String): Boolean {
        val currentEntitlement = getAccountEntitlement(accountId)
        if (com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(
                com.example.emat.domain.service.EmatFeature.INSTRUMENT_REGISTRY_MUTATION,
                currentEntitlement.state
            )
        ) {
            logAuditEvent(
                accountId = accountId,
                category = "ENTITLEMENT",
                action = "FEATURE_ACCESS_DENIED",
                actor = "REPOSITORY",
                rationale = "Instrument toggle blocked: account entitlement state ${currentEntitlement.state.name} does not permit registry mutation",
                dataAvailable = "Feature: INSTRUMENT_REGISTRY_MUTATION, State: ${currentEntitlement.state.name}",
                decisionMade = "Reject instrument toggle at repository boundary",
                outcome = "INSTRUMENT_GATED"
            )
            return false
        }
        db.instrumentDao().setInstrumentActive(symbol, isActive)
        logAuditEvent(
            accountId = "SYSTEM",
            category = "SYSTEM",
            action = "CONFIG_INSTRUMENT",
            actor = "OPERATOR",
            rationale = "Instrument active toggle updated",
            dataAvailable = "Symbol: $symbol, new isActive: $isActive",
            decisionMade = if (isActive) "Activated for scanner" else "Deactivated from scanner",
            outcome = "Registry updated"
        )
        return true
    }

    override suspend fun generateAiAnalysis(
        accountId: String,
        contextTitle: String,
        activeTrades: Int,
        isOffline: Boolean,
        selectedOpportunity: Opportunity?
    ): com.example.emat.domain.model.AiAnalysisResult? {
        val currentEntitlement = getAccountEntitlement(accountId)
        if (com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(
                com.example.emat.domain.service.EmatFeature.AI_DECISION_ANALYSIS,
                currentEntitlement.state
            )
        ) {
            logAuditEvent(
                accountId = accountId,
                category = "ENTITLEMENT",
                action = "FEATURE_ACCESS_DENIED",
                actor = "REPOSITORY",
                rationale = "AI analysis blocked: account entitlement state ${currentEntitlement.state.name} does not permit AI decision analysis",
                dataAvailable = "Feature: AI_DECISION_ANALYSIS, State: ${currentEntitlement.state.name}",
                decisionMade = "Reject AI analysis at repository boundary",
                outcome = "AI_GATED"
            )
            return null
        }
        return com.example.emat.domain.service.EmatAiEngine().generateAnalysis(
            context = contextTitle,
            activeTrades = activeTrades,
            isEmergencyStopped = isOffline,
            topOpp = selectedOpportunity
        )
    }

    override fun getTopOpportunitiesFlow(): Flow<List<Opportunity>> {
        return db.scannerDao().getTopOpportunities().map { entities ->
            entities.map { entity ->
                val now = System.currentTimeMillis()
                val freshnessSec = (now - entity.timestamp) / 1000L
                val isStale = freshnessSec > 15L

                Opportunity(
                    rank = entity.rank,
                    symbol = entity.symbol,
                    assetClass = try { AssetClass.valueOf(entity.assetClass) } catch (e: Exception) { AssetClass.FOREX },
                    direction = when (entity.direction) {
                        "BUY" -> TradeDirection.BUY
                        "SELL" -> TradeDirection.SELL
                        else -> TradeDirection.NONE
                    },
                    eceScore = entity.eceScore,
                    marketCondition = when (entity.marketCondition) {
                        "TRENDING_BULLISH" -> MarketCondition.TRENDING_BULLISH
                        "TRENDING_BEARISH" -> MarketCondition.TRENDING_BEARISH
                        "ACCUMULATION" -> MarketCondition.ACCUMULATION
                        "EXPANSION" -> MarketCondition.EXPANSION
                        else -> MarketCondition.RANGING
                    },
                    setupStatus = when (entity.setupStatus) {
                        "QUALIFIED" -> SetupStatus.QUALIFIED
                        "MONITORING" -> SetupStatus.MONITORING
                        "INVALIDATED" -> SetupStatus.INVALIDATED
                        else -> SetupStatus.NO_SETUP
                    },
                    timeframeAlignment = when (entity.timeframeAlignment) {
                        "FULL_ALIGNMENT" -> TimeframeAlignment.FULL_ALIGNMENT
                        "PARTIAL_ALIGNMENT" -> TimeframeAlignment.PARTIAL_ALIGNMENT
                        "DIVERGENT" -> TimeframeAlignment.DIVERGENT
                        else -> TimeframeAlignment.UNCONFIRMED
                    },
                    riskStatus = when (entity.riskStatus) {
                        "APPROVED" -> RiskStatus.APPROVED
                        "BLOCKED_ONE_TRADE_RULE" -> RiskStatus.BLOCKED_ONE_TRADE_RULE
                        "AWAITING_REVIEW" -> RiskStatus.AWAITING_REVIEW
                        else -> RiskStatus.REJECTED
                    },
                    whyItQualifies = entity.whyItQualifies,
                    timestamp = entity.timestamp,
                    freshnessSeconds = freshnessSec,
                    isStale = isStale,
                    dataOrigin = when {
                        isStale -> DataOrigin.UNAVAILABLE
                        marketDataProvider.getDataState() == MarketDataState.CONNECTED -> DataOrigin.LIVE_FEED
                        else -> DataOrigin.UNAVAILABLE
                    }
                )
            }
        }
    }

    override fun getActiveTradeFlow(): Flow<ActiveTrade?> {
        return db.tradeDao().getActiveTradeFlow().map { entity ->
            entity?.let { mapToActiveTrade(it) }
        }
    }

    override fun getActiveTradeByAccountFlow(accountId: String): Flow<ActiveTrade?> {
        return db.tradeDao().getActiveTradeByAccountFlow(accountId).map { entity ->
            entity?.let { mapToActiveTrade(it) }
        }
    }

    private fun mapToActiveTrade(it: TradeEntity): ActiveTrade {
        return ActiveTrade(
            tradeId = it.tradeId,
            symbol = it.symbol,
            direction = if (it.direction == "BUY") TradeDirection.BUY else TradeDirection.SELL,
            entryPrice = it.entryPrice,
            stopLoss = it.stopLoss,
            takeProfit = it.takeProfit,
            positionSizeLots = it.positionSize,
            currentPrice = it.currentPrice,
            unrealizedPnl = it.unrealizedResult,
            isBreakEven = it.isBreakEven,
            riskStatus = RiskStatus.APPROVED,
            status = it.status,
            executionMode = it.executionMode,
            openedAt = it.openedAt
        )
    }

    override fun getAuditLogsFlow(): Flow<List<AuditLogEntity>> =
        db.auditLogDao().getRecentAuditLogs()

    override fun getAuditLogsByAccountFlow(accountId: String): Flow<List<AuditLogEntity>> =
        db.auditLogDao().getRecentAuditLogsByAccount(accountId)

    override fun getEvidenceByAccountFlow(accountId: String): Flow<List<EvidenceRecord>> {
        return db.evidenceDao().getEvidenceByAccount(accountId).map { list ->
            list.map {
                EvidenceRecord(
                    evidenceId = it.evidenceId,
                    instrument = it.instrument,
                    timeframe = it.timeframe,
                    sourceRef = it.sourceRef,
                    timestamp = it.timestamp,
                    observation = it.observation,
                    calculation = it.calculation,
                    interpretation = it.interpretation,
                    relatedOpportunityId = it.relatedOpportunityId,
                    relatedTradeId = it.relatedTradeId,
                    verificationStatus = it.verificationStatus
                )
            }
        }
    }

    override fun getSystemHealthFlow(): Flow<List<SystemHealthEntity>> =
        db.systemHealthDao().getAllHealthFlow()

    override suspend fun isOneTradeRuleViolated(accountId: String): Boolean {
        // Global One-Trade Rule: at most one active trade across all accounts
        return db.tradeDao().getActiveTradeCount() >= 1
    }

    override suspend fun executeMarketScan(dataProviderType: String, accountId: String): List<Opportunity> {
        val startTime = System.currentTimeMillis()
        val runId = "SCAN-${UUID.randomUUID().toString().take(8)}"

        // Fail-safe check: Emergency Stop active
        if (executionEngine.isEmergencyStopped()) {
            logAuditEvent(
                accountId = accountId,
                category = "SCANNER",
                action = "SCAN_ABORTED",
                actor = "SCANNER_ENGINE",
                rationale = "Emergency stop active across platform",
                dataAvailable = "ExecutionState = EMERGENCY_STOP",
                decisionMade = "Abort universe scan immediately",
                outcome = "Zero opportunities returned"
            )
            return emptyList()
        }

        // Market-data truth check: If data provider is explicitly NOT_CONNECTED, do not fabricate prices
        if (dataProviderType == "NOT_CONNECTED") {
            logAuditEvent(
                accountId = accountId,
                category = "DATA",
                action = "SCAN_BLOCKED_DATA_NOT_CONNECTED",
                actor = "SCANNER_ENGINE",
                rationale = "No market data feed is connected to platform",
                dataAvailable = "MarketDataState = NOT_CONNECTED",
                decisionMade = "Halt scan to prevent trading or ranking on nonexistent data",
                outcome = "Zero opportunities returned"
            )
            return emptyList()
        }

        // Repository-level Entitlement Gating
        val currentEntitlement = getAccountEntitlement(accountId)
        if (com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(
                com.example.emat.domain.service.EmatFeature.FULL_MARKET_SCAN,
                currentEntitlement.state
            )
        ) {
            logAuditEvent(
                accountId = accountId,
                category = "ENTITLEMENT",
                action = "FEATURE_ACCESS_DENIED",
                actor = "REPOSITORY",
                rationale = "Market scan blocked: account entitlement state ${currentEntitlement.state.name} does not permit full market scan",
                dataAvailable = "State: ${currentEntitlement.state.name}",
                decisionMade = "Reject market scan at repository boundary",
                outcome = "SCANNER_GATED"
            )
            return emptyList()
        }

        val staleThresholdSec = db.configDao().getConfig("DATA_STALE_THRESHOLD_SEC")?.value?.toLongOrNull() ?: 15L
        if (dataProviderType == "LIVE") {
            val evaluatedState = marketDataProvider.evaluateFreshness(System.currentTimeMillis(), staleThresholdSec)
            if (evaluatedState == MarketDataState.NOT_CONNECTED ||
                evaluatedState == MarketDataState.STALE ||
                evaluatedState == MarketDataState.RATE_LIMITED ||
                evaluatedState == MarketDataState.ERROR
            ) {
                logAuditEvent(
                    accountId = accountId,
                    category = "DATA",
                    action = "SCAN_BLOCKED_DATA_NOT_CONNECTED",
                    actor = "SCANNER_ENGINE",
                    rationale = "Live market data feed is ${evaluatedState.name}; scan blocked",
                    dataAvailable = "MarketDataState = ${evaluatedState.name}",
                    decisionMade = "Halt scan to prevent trading on stale or disconnected data",
                    outcome = "Zero opportunities returned"
                )
                return emptyList()
            }
        }

        val activeInstruments = db.instrumentDao().getActiveInstruments()
        val scannedCount = activeInstruments.size
        val activeTradesCount = db.tradeDao().getActiveTradeCount()

        val qualifiedList = mutableListOf<Opportunity>()

        // Authoritative base references for realistic structural evaluation in TEST MODE
        val basePrices = mapOf(
            "EURUSD" to 1.0850, "GBPUSD" to 1.2720, "USDJPY" to 154.20, "USDCHF" to 0.8920,
            "USDCAD" to 1.3650, "AUDUSD" to 0.6580, "NZDUSD" to 0.6120, "EURGBP" to 0.8530,
            "EURJPY" to 167.30, "GBPJPY" to 196.10, "XAUUSD" to 2420.50, "XAGUSD" to 29.40,
            "NAS100" to 19850.0, "US500" to 5520.0, "US30" to 39800.0, "GER40" to 18350.0,
            "UK100" to 8240.0, "BTCUSD" to 63500.0, "ETHUSD" to 3420.0
        )

        val marketDataEntities = mutableListOf<MarketDataEntity>()

        for ((index, inst) in activeInstruments.withIndex()) {
            val open: Double
            val high: Double
            val low: Double
            val close: Double
            val spreadPips: Double
            val bid: Double
            val ask: Double
            val volume: Double
            val quoteTimestamp: Long

            if (dataProviderType == "LIVE") {
                val quoteRes = marketDataProvider.fetchQuote(inst.symbol)
                val candlesRes = marketDataProvider.fetchCandles(inst.symbol, "15min", 30)
                if (quoteRes.isFailure || candlesRes.isFailure) {
                    continue
                }
                val quote = quoteRes.getOrThrow()
                val candles = candlesRes.getOrThrow()
                if (candles.isEmpty()) continue
                val latest = candles.first()
                bid = quote.bid
                ask = quote.ask
                spreadPips = if (inst.pipSize > 0.0) (ask - bid) / inst.pipSize else 0.0
                open = latest.open
                high = candles.maxOf { it.high }
                low = candles.minOf { it.low }
                close = latest.close
                volume = latest.volume
                quoteTimestamp = quote.sourceTimestamp
            } else {
                val base = basePrices[inst.symbol] ?: 100.0
                val seedFactor = ((startTime / 15000) + index * 23) % 100
                val delta = (seedFactor - 50) / 1000.0 * base * 0.015

                high = base + (base * 0.008)
                low = base - (base * 0.008)
                close = base + delta
                open = base
                spreadPips = when (inst.assetClass) {
                    "FOREX" -> 1.2
                    "METALS" -> 2.5
                    "INDICES" -> 1.5
                    "CRYPTO" -> 3.2
                    else -> 1.5
                }
                val spreadUnits = spreadPips * inst.pipSize
                bid = close
                ask = close + spreadUnits
                volume = 12000.0
                quoteTimestamp = startTime
            }

            marketDataEntities.add(
                MarketDataEntity(
                    symbol = inst.symbol,
                    bid = bid,
                    ask = ask,
                    spread = spreadPips,
                    timeframe = "H1",
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume,
                    dataProvider = dataProviderType,
                    timestamp = quoteTimestamp,
                    isStale = false
                )
            )

            // Step 1: Strategy Engine Evaluation
            val assetClass = try { AssetClass.valueOf(inst.assetClass) } catch (e: Exception) { AssetClass.FOREX }
            val strategyResult = strategyEngine.evaluateSetup(
                symbol = inst.symbol,
                assetClass = assetClass,
                bid = bid,
                ask = ask,
                spread = spreadPips,
                high = high,
                low = low,
                close = close
            )

            // Step 2: ECE Engine Score Calculation
            val isNewsImminent = newsService.isNewsImminentForInstrument(inst.symbol)
            val eceScore = eceEngine.calculateScore(strategyResult, spreadPips, isNewsImminent)

            // Step 3: Risk Engine Evaluation (Independent check)
            val riskEval = riskEngine.evaluateOpportunity(
                symbol = inst.symbol,
                eceScore = eceScore.overall,
                activeTradesCount = activeTradesCount,
                spreadPips = spreadPips,
                isEmergencyStopped = executionEngine.isEmergencyStopped(),
                maxActiveTrades = 1
            )

            // Record ECE Score in DB
            db.eceScoreDao().insertScore(
                EceScoreEntity(
                    symbol = inst.symbol,
                    overallEce = eceScore.overall,
                    trendScore = eceScore.trendScore,
                    structureScore = eceScore.structureScore,
                    momentumScore = eceScore.momentumScore,
                    confirmationScore = eceScore.confirmationScore,
                    timeframeScore = eceScore.timeframeScore,
                    setupQuality = eceScore.setupQuality,
                    riskScore = eceScore.riskScore,
                    newsRiskScore = eceScore.newsRiskScore,
                    explanation = eceScore.explanation,
                    timestamp = startTime
                )
            )

            // Record Risk Check in DB
            db.riskCheckDao().insertRiskCheck(
                RiskCheckEntity(
                    symbol = inst.symbol,
                    decision = if (riskEval.isApproved) "APPROVED" else "REJECTED",
                    accountRiskPercent = riskEval.accountRiskPercent,
                    positionSize = riskEval.calculatedPositionSize,
                    stopLossDistancePips = 25.0,
                    spreadPips = spreadPips,
                    activeTradesCount = activeTradesCount,
                    maxAllowedTrades = 1,
                    rejectionReason = riskEval.rejectionReason,
                    timestamp = startTime
                )
            )

            // Setup strictly qualifies ONLY if strategy confirms QUALIFIED and ECE score meets institutional threshold (>= 70)
            if (strategyResult.setupStatus == SetupStatus.QUALIFIED && eceScore.overall >= 70) {
                qualifiedList.add(
                    Opportunity(
                        rank = 0, // Assigned after sorting
                        symbol = inst.symbol,
                        assetClass = assetClass,
                        direction = strategyResult.direction,
                        eceScore = eceScore.overall,
                        marketCondition = strategyResult.marketCondition,
                        setupStatus = strategyResult.setupStatus,
                        timeframeAlignment = strategyResult.timeframeAlignment,
                        riskStatus = riskEval.status,
                        whyItQualifies = "${strategyResult.rationale} ${eceScore.explanation}",
                        timestamp = startTime,
                        freshnessSeconds = 0,
                        isStale = false
                    )
                )
            }
        }

        // Save Market Data
        db.marketDataDao().insertMarketData(marketDataEntities)

        // DYNAMIC RANKING: Sort qualifying opportunities strictly by ECE Score descending
        val sortedQualifying = qualifiedList.sortedByDescending { it.eceScore }

        // Take Top 3. If fewer than 3 qualify, explicitly output "NONE" for remaining slots. Never fabricate!
        val topThreeList = mutableListOf<Opportunity>()
        for (i in 0 until 3) {
            if (i < sortedQualifying.size) {
                val item = sortedQualifying[i].copy(rank = i + 1)
                topThreeList.add(item)
            } else {
                // Explicit slot representing NO qualifying setup
                topThreeList.add(
                    Opportunity(
                        rank = i + 1,
                        symbol = "NONE",
                        assetClass = AssetClass.FOREX,
                        direction = TradeDirection.NONE,
                        eceScore = 0,
                        marketCondition = MarketCondition.RANGING,
                        setupStatus = SetupStatus.NO_SETUP,
                        timeframeAlignment = TimeframeAlignment.UNCONFIRMED,
                        riskStatus = RiskStatus.AWAITING_REVIEW,
                        whyItQualifies = "No qualifying setup satisfies EMAT criteria for Rank ${i + 1}.",
                        timestamp = startTime,
                        freshnessSeconds = 0,
                        isStale = false
                    )
                )
            }
        }

        // Persist Opportunities to Database
        db.scannerDao().clearOpportunities()
        db.scannerDao().insertOpportunities(
            topThreeList.map {
                OpportunityEntity(
                    scanRunId = runId,
                    rank = it.rank,
                    symbol = it.symbol,
                    assetClass = it.assetClass.name,
                    direction = it.direction.name,
                    eceScore = it.eceScore,
                    marketCondition = it.marketCondition.name,
                    setupStatus = it.setupStatus.name,
                    timeframeAlignment = it.timeframeAlignment.name,
                    riskStatus = it.riskStatus.name,
                    whyItQualifies = it.whyItQualifies,
                    timestamp = it.timestamp,
                    freshnessSeconds = 0
                )
            }
        )

        // Record Scanner Run in Database
        val duration = System.currentTimeMillis() - startTime
        db.scannerDao().insertScannerRun(
            ScannerRunEntity(
                runId = runId,
                timestamp = startTime,
                instrumentsScanned = scannedCount,
                instrumentsQualifying = sortedQualifying.size,
                status = "COMPLETED",
                durationMs = duration
            )
        )

        // Record Evidence for Top Qualified Setups
        topThreeList.filter { it.symbol != "NONE" }.forEach { opp ->
            val evd = EvidenceEntity(
                evidenceId = "EVD-${opp.symbol}-${opp.rank}-${startTime.toString().takeLast(4)}",
                accountId = accountId,
                instrument = opp.symbol,
                timeframe = "H4 / H1",
                sourceRef = "SCANNER_ENGINE_$dataProviderType",
                timestamp = startTime,
                observation = "Qualified setup #${opp.rank} identified on ${opp.symbol} (${opp.direction}) with ECE ${opp.eceScore}/100.",
                calculation = "ECE weighted multi-factor aggregate = ${opp.eceScore}. Timeframe alignment: ${opp.timeframeAlignment.label}.",
                interpretation = opp.whyItQualifies,
                relatedOpportunityId = "${opp.symbol}-${opp.rank}",
                verificationStatus = "AUDITED_QUALIFIED"
            )
            db.evidenceDao().insertEvidence(evd)
        }

        // Record Mandatory Audit Log
        val topSymbols = topThreeList.filter { it.symbol != "NONE" }.joinToString { "${it.symbol} (ECE ${it.eceScore})" }
        logAuditEvent(
            accountId = accountId,
            category = "SCANNER",
            action = "UNIVERSE_SCAN_COMPLETED",
            actor = "SCANNER_ENGINE",
            rationale = "Evaluated all $scannedCount active instruments across Forex, Metals, Indices, Crypto",
            dataAvailable = "Scanned $scannedCount active instruments, $dataProviderType feed, duration: ${duration}ms",
            decisionMade = "Identified ${sortedQualifying.size} qualifying setups; Ranked Top 3: [${if (topSymbols.isEmpty()) "None qualified" else topSymbols}]",
            outcome = "Stored top opportunities and updated scanner state"
        )

        return topThreeList
    }

    override suspend fun triggerEmergencyStop() {
        executionEngine.triggerEmergencyStop()
        db.tradeDao().emergencyStopAllTrades()
        db.systemHealthDao().insertOrUpdateHealth(
            SystemHealthEntity("EXECUTION", "WARNING", System.currentTimeMillis(), "Emergency stop engaged by operator", 0)
        )
        logAuditEvent(
            accountId = "SYSTEM",
            category = "EMERGENCY",
            action = "EMERGENCY_STOP_TRIGGERED",
            actor = "OPERATOR",
            rationale = "Immediate platform halt invoked by operator",
            dataAvailable = "ExecutionState = EMERGENCY_STOP",
            decisionMade = "Halted execution and cancelled active positions",
            outcome = "Fail-safe posture activated"
        )
    }

    override suspend fun resetEmergencyStop() {
        executionEngine.resetEmergencyStop()
        db.systemHealthDao().insertOrUpdateHealth(
            SystemHealthEntity("EXECUTION", "NOT_CONNECTED", System.currentTimeMillis(), "Broker adapter in NOT_CONNECTED state", 0)
        )
        logAuditEvent(
            accountId = "SYSTEM",
            category = "EMERGENCY",
            action = "EMERGENCY_STOP_RESET",
            actor = "OPERATOR",
            rationale = "Manual reset from emergency stop",
            dataAvailable = "ExecutionState reset to default NOT_CONNECTED",
            decisionMade = "Return to nominal disconnected monitoring",
            outcome = "Platform resumed monitoring"
        )
    }

    override suspend fun updateSystemHealth(component: String, status: ComponentHealth, details: String) {
        db.systemHealthDao().insertOrUpdateHealth(
            SystemHealthEntity(
                componentKey = component,
                status = status.name,
                lastCheckTime = System.currentTimeMillis(),
                details = details,
                errorCount = if (status == ComponentHealth.ERROR) 1 else 0
            )
        )
    }

    override suspend fun logAuditEvent(
        accountId: String,
        category: String,
        action: String,
        actor: String,
        rationale: String,
        dataAvailable: String,
        decisionMade: String,
        outcome: String,
        eventMetadata: String
    ) {
        val entry = AuditLogEntity.createSecureEntry(
            accountId = accountId,
            category = category,
            action = action,
            actor = actor,
            rationale = rationale,
            dataAvailable = dataAvailable,
            decisionMade = decisionMade,
            outcome = outcome,
            eventMetadata = eventMetadata
        )
        db.auditLogDao().insertAuditLog(entry)
    }

    private val tradeSubmissionMutex = Mutex()

    override suspend fun submitTradeSafely(trade: TradeEntity): Boolean {
        return tradeSubmissionMutex.withLock {
            // Duplicate tradeId check
            val allTrades = db.tradeDao().getAllTrades().first()
            if (allTrades.any { it.tradeId == trade.tradeId }) {
                logAuditEvent(
                    accountId = trade.accountId,
                    category = "RISK",
                    action = "TRADE_REJECTED_DUPLICATE_ID",
                    actor = "REPOSITORY_MUTEX",
                    rationale = "Order rejected: duplicate tradeId '${trade.tradeId}' already exists in ledger",
                    dataAvailable = "TradeId: ${trade.tradeId}, Symbol: ${trade.symbol}",
                    decisionMade = "Reject duplicate trade submission",
                    outcome = "Duplicate rejected"
                )
                return@withLock false
            }

            // Global One-Trade Rule check: at most one active trade across all accounts
            val globalActiveCount = db.tradeDao().getActiveTradeCount()
            if (globalActiveCount >= 1) {
                logAuditEvent(
                    accountId = trade.accountId,
                    category = "RISK",
                    action = "TRADE_REJECTED_ONE_TRADE_RULE",
                    actor = "REPOSITORY_MUTEX",
                    rationale = "Global One-Trade Rule active: terminal already has $globalActiveCount active trade across platform",
                    dataAvailable = "TradeId: ${trade.tradeId}, Symbol: ${trade.symbol}, GlobalActiveCount: $globalActiveCount",
                    decisionMade = "Reject order: global platform maximum of 1 active trade reached",
                    outcome = "Global One-Trade Rule invariant enforced"
                )
                false
            } else {
                db.tradeDao().insertTrade(trade)
                logAuditEvent(
                    accountId = trade.accountId,
                    category = "TRADE",
                    action = "TRADE_PLACED",
                    actor = "OPERATOR",
                    rationale = "New trade inserted within 1-Trade Rule concurrency critical section",
                    dataAvailable = "TradeId: ${trade.tradeId}, Symbol: ${trade.symbol}, Lots: ${trade.positionSize}",
                    decisionMade = "Order accepted and registered in local database",
                    outcome = "Trade active"
                )
                true
            }
        }
    }

    override suspend fun runDatabaseIntegrityCheck(): DbIntegrityResult = withContext(Dispatchers.IO) {
        try {
            val sdb = db.openHelper.readableDatabase
            val version = sdb.version
            val cursor = sdb.query("PRAGMA quick_check")
            var resultText = ""
            if (cursor.moveToFirst()) {
                resultText = cursor.getString(0) ?: ""
            }
            cursor.close()
            val isOk = resultText.trim().equals("ok", ignoreCase = true)
            val now = System.currentTimeMillis()
            val status = if (isOk) "INTEGRITY OK" else "INTEGRITY CHECK FAILED"
            DbIntegrityResult(
                status = status,
                isOk = isOk,
                version = version,
                timestamp = now,
                details = resultText
            )
        } catch (e: Exception) {
            DbIntegrityResult(
                status = "INTEGRITY CHECK FAILED",
                isOk = false,
                version = 0,
                timestamp = System.currentTimeMillis(),
                details = e.message ?: "Integrity check exception"
            )
        }
    }

    override suspend fun runAiAnalyzerSelfCheck(): Boolean = withContext(Dispatchers.Default) {
        try {
            val engine = com.example.emat.domain.service.EmatAiEngine()
            val result = engine.generateAnalysis("SELF_CHECK_FIXTURE", 0, false, null)
            result.contextTitle == "SELF_CHECK_FIXTURE" &&
                result.fact.isNotBlank() &&
                result.calculation.isNotBlank() &&
                result.analysis.isNotBlank() &&
                result.inference.isNotBlank() &&
                result.recommendation.isNotBlank()
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun logEvidence(record: EvidenceRecord, accountId: String) {
        db.evidenceDao().insertEvidence(
            EvidenceEntity(
                evidenceId = record.evidenceId,
                accountId = accountId,
                instrument = record.instrument,
                timeframe = record.timeframe,
                sourceRef = record.sourceRef,
                timestamp = record.timestamp,
                observation = record.observation,
                calculation = record.calculation,
                interpretation = record.interpretation,
                relatedOpportunityId = record.relatedOpportunityId,
                relatedTradeId = record.relatedTradeId,
                verificationStatus = record.verificationStatus
            )
        )
    }

    private val trialStartMutex = Mutex()

    private fun deriveEntitlement(
        entity: AccountEntitlementEntity?,
        accountId: String,
        now: Long
    ): AccountEntitlement {
        if (entity == null) {
            return AccountEntitlement(
                accountId = accountId,
                state = EntitlementState.NOT_SUBSCRIBED,
                remainingTrialMillis = 0L
            )
        }

        // 1. Check verified subscription
        if (entity.verifiedByBillingProvider && entity.subscriptionExpiresAt != null && now < entity.subscriptionExpiresAt) {
            return AccountEntitlement(
                accountId = accountId,
                state = EntitlementState.SUBSCRIPTION_ACTIVE,
                trialStartedAt = entity.trialStartedAt,
                trialDurationDays = entity.trialDurationDays,
                accumulatedTrialDurationMillis = entity.accumulatedTrialDurationMillis,
                lastObservedTimestamp = entity.lastObservedTimestamp,
                hasClockAnomaly = entity.hasClockAnomaly,
                subscriptionId = entity.subscriptionId,
                subscriptionPlanId = entity.subscriptionPlanId,
                subscriptionPurchasedAt = entity.subscriptionPurchasedAt,
                subscriptionExpiresAt = entity.subscriptionExpiresAt,
                verifiedByBillingProvider = true,
                remainingTrialMillis = 0L
            )
        }

        if (entity.subscriptionExpiresAt != null && now >= entity.subscriptionExpiresAt) {
            return AccountEntitlement(
                accountId = accountId,
                state = EntitlementState.SUBSCRIPTION_EXPIRED,
                trialStartedAt = entity.trialStartedAt,
                trialDurationDays = entity.trialDurationDays,
                accumulatedTrialDurationMillis = entity.accumulatedTrialDurationMillis,
                lastObservedTimestamp = entity.lastObservedTimestamp,
                hasClockAnomaly = entity.hasClockAnomaly,
                subscriptionId = entity.subscriptionId,
                subscriptionPlanId = entity.subscriptionPlanId,
                subscriptionPurchasedAt = entity.subscriptionPurchasedAt,
                subscriptionExpiresAt = entity.subscriptionExpiresAt,
                verifiedByBillingProvider = entity.verifiedByBillingProvider,
                remainingTrialMillis = 0L
            )
        }

        // 2. Check trial
        if (entity.trialStartedAt != null) {
            val totalAllowedMillis = entity.trialDurationDays * 24 * 60 * 60 * 1000L
            val additionalElapsed = if (now > entity.lastObservedTimestamp) {
                now - entity.lastObservedTimestamp
            } else {
                0L
            }
            val effectiveAccumulated = entity.accumulatedTrialDurationMillis + additionalElapsed
            val remainingMillis = maxOf(0L, totalAllowedMillis - effectiveAccumulated)
            val isAnomaly = entity.hasClockAnomaly || (now < entity.lastObservedTimestamp)

            val state = if (remainingMillis <= 0L) {
                EntitlementState.TRIAL_EXPIRED
            } else {
                EntitlementState.TRIAL_ACTIVE
            }

            return AccountEntitlement(
                accountId = accountId,
                state = state,
                trialStartedAt = entity.trialStartedAt,
                trialDurationDays = entity.trialDurationDays,
                accumulatedTrialDurationMillis = effectiveAccumulated,
                lastObservedTimestamp = entity.lastObservedTimestamp,
                hasClockAnomaly = isAnomaly,
                subscriptionId = entity.subscriptionId,
                subscriptionPlanId = entity.subscriptionPlanId,
                subscriptionPurchasedAt = entity.subscriptionPurchasedAt,
                subscriptionExpiresAt = entity.subscriptionExpiresAt,
                verifiedByBillingProvider = entity.verifiedByBillingProvider,
                remainingTrialMillis = remainingMillis
            )
        }

        return AccountEntitlement(
            accountId = accountId,
            state = EntitlementState.NOT_SUBSCRIBED,
            remainingTrialMillis = 0L
        )
    }

    override fun getAccountEntitlementFlow(accountId: String): Flow<AccountEntitlement> {
        return db.entitlementDao().getEntitlementFlow(accountId).map { entity ->
            deriveEntitlement(entity, accountId, System.currentTimeMillis())
        }
    }

    override suspend fun getAccountEntitlement(accountId: String, now: Long): AccountEntitlement {
        val entity = db.entitlementDao().getEntitlement(accountId)
        return deriveEntitlement(entity, accountId, now)
    }

    override suspend fun refreshEntitlementObservation(accountId: String, now: Long): AccountEntitlement {
        val entity = db.entitlementDao().getEntitlement(accountId) ?: return AccountEntitlement(
            accountId = accountId,
            state = EntitlementState.NOT_SUBSCRIBED,
            remainingTrialMillis = 0L
        )

        val lastObserved = entity.lastObservedTimestamp
        val (updatedAccumulated, updatedLastObserved, updatedAnomaly) = when {
            now < lastObserved -> {
                Triple(entity.accumulatedTrialDurationMillis, lastObserved, true)
            }
            else -> {
                val delta = now - lastObserved
                val newAccum = entity.accumulatedTrialDurationMillis + delta
                Triple(newAccum, now, false)
            }
        }

        val updatedEntity = entity.copy(
            accumulatedTrialDurationMillis = updatedAccumulated,
            lastObservedTimestamp = updatedLastObserved,
            hasClockAnomaly = updatedAnomaly,
            updatedAt = now
        )
        db.entitlementDao().updateEntitlement(updatedEntity)

        if (updatedAnomaly && !entity.hasClockAnomaly) {
            logAuditEvent(
                accountId = accountId,
                category = "ENTITLEMENT",
                action = "CLOCK_ANOMALY_DETECTED",
                actor = "SYSTEM",
                rationale = "Device wall-clock moved backward from $lastObserved to $now",
                dataAvailable = "Discrepancy: ${lastObserved - now} ms",
                decisionMade = "Lock forward progress; trial extension refused",
                outcome = "Clock anomaly flag raised"
            )
        }

        val derived = deriveEntitlement(updatedEntity, accountId, now)
        if (derived.state == EntitlementState.TRIAL_EXPIRED) {
            val existingLogs = db.auditLogDao().getRecentAuditLogsByAccount(accountId).first()
            if (existingLogs.none { it.action == "TRIAL_EXPIRED" }) {
                logAuditEvent(
                    accountId = accountId,
                    category = "ENTITLEMENT",
                    action = "TRIAL_EXPIRED",
                    actor = "SYSTEM",
                    rationale = "14-day trial duration elapsed for account $accountId",
                    dataAvailable = "Accumulated: ${derived.accumulatedTrialDurationMillis} ms",
                    decisionMade = "Transition derived entitlement to TRIAL_EXPIRED",
                    outcome = "TRIAL_EXPIRED"
                )
            }
        }

        return derived
    }

    override suspend fun startTrialSafely(accountId: String): Boolean = trialStartMutex.withLock {
        val existing = db.entitlementDao().getEntitlement(accountId)
        if (existing != null && existing.trialStartedAt != null) {
            return false // Refused: trial already started or exists for this account
        }
        val now = System.currentTimeMillis()
        val newEntitlement = AccountEntitlementEntity(
            accountId = accountId,
            trialStartedAt = now,
            trialDurationDays = DEFAULT_TRIAL_DURATION_DAYS,
            accumulatedTrialDurationMillis = 0L,
            lastObservedTimestamp = now,
            hasClockAnomaly = false,
            subscriptionId = null,
            subscriptionPlanId = null,
            subscriptionPurchasedAt = null,
            subscriptionExpiresAt = null,
            verifiedByBillingProvider = false,
            updatedAt = now
        )
        try {
            db.entitlementDao().insertEntitlement(newEntitlement)
            logAuditEvent(
                accountId = accountId,
                category = "ENTITLEMENT",
                action = "TRIAL_STARTED",
                actor = "OPERATOR",
                rationale = "User initiated 14-day trial on account $accountId",
                dataAvailable = "Duration: $DEFAULT_TRIAL_DURATION_DAYS days",
                decisionMade = "Grant trial entitlement",
                outcome = "TRIAL_ACTIVE"
            )
            true
        } catch (_: Exception) {
            false
        }
    }
}
