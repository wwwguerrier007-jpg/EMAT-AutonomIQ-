package com.example.emat.domain.service

import com.example.emat.data.local.entity.MarketDataEntity
import com.example.emat.domain.model.AssetClass
import com.example.emat.domain.model.EceScoreBreakdown
import com.example.emat.domain.model.MarketCondition
import com.example.emat.domain.model.SetupStatus
import com.example.emat.domain.model.TimeframeAlignment
import com.example.emat.domain.model.TradeDirection
import kotlin.math.abs

/**
 * Institutional Market Analyzer utility class.
 *
 * Encapsulates the quantitative calculation of the ECE (Efficiency, Confidence, Exposure)
 * framework for financial assets to ensure objective, verifiable, and consistent risk assessment.
 *
 * ECE Framework Definition:
 * - **Efficiency (E)**: Evaluates price action structure, candle displacement relative to range,
 *   and execution friction / spread drag (0-100).
 * - **Confidence (C)**: Evaluates directional conviction, multi-timeframe alignment (H4/H1/M15),
 *   and technical confirmation confluence (0-100).
 * - **Exposure (E)**: Evaluates capital safety, proximity to macro news shockwaves, stop-loss viability,
 *   and market volatility exposure (0-100).
 */
class MarketAnalyzer {

    enum class NewsFeedStatus {
        VERIFIED_CLEAR,
        IMMINENT_HIGH_IMPACT,
        UNVERIFIED_OR_MISSING
    }

    data class MultiTimeframeEvidence(
        val h4Trend: TradeDirection,
        val h1Trend: TradeDirection,
        val m15Trend: TradeDirection,
        val isEvidenceVerified: Boolean = true
    )

    data class EceAnalysisResult(
        val symbol: String,
        val efficiencyScore: Int,
        val confidenceScore: Int,
        val exposureScore: Int,
        val compositeEceScore: Int,
        val breakdown: EceScoreBreakdown,
        val status: SetupStatus,
        val direction: TradeDirection,
        val marketCondition: MarketCondition,
        val timeframeAlignment: TimeframeAlignment,
        val riskAssessment: String,
        val isActionable: Boolean,
        val missingEvidenceReason: String? = null
    )

    companion object {
        const val QUALIFIED_THRESHOLD = 70
        const val MONITORING_THRESHOLD = 50
        const val MINIMUM_EXPOSURE_QUALIFICATION = 60
        const val MAX_ALLOWED_SPREAD_PIPS = 6.0
        const val WEIGHT_EFFICIENCY = 0.35
        const val WEIGHT_CONFIDENCE = 0.40
        const val WEIGHT_EXPOSURE = 0.25

        val Default: MarketAnalyzer by lazy { MarketAnalyzer() }
    }

    private val strategyEngine = StrategyEngine()

    /**
     * Validates raw market quotes and candle structures.
     * Returns an error description if inputs are invalid or corrupt, or null if valid.
     */
    fun validateMarketInput(
        bid: Double,
        ask: Double,
        spread: Double,
        high: Double,
        low: Double,
        close: Double
    ): String? {
        if (bid.isNaN() || bid.isInfinite() || bid <= 0.0) return "Invalid bid price ($bid): must be a positive finite number."
        if (ask.isNaN() || ask.isInfinite() || ask <= 0.0) return "Invalid ask price ($ask): must be a positive finite number."
        if (ask < bid) return "Inverted quote: ask ($ask) is less than bid ($bid)."
        if (spread.isNaN() || spread.isInfinite() || spread < 0.0) return "Invalid spread ($spread): must be non-negative and finite."
        if (high.isNaN() || high.isInfinite() || high <= 0.0) return "Invalid candle high ($high): must be positive and finite."
        if (low.isNaN() || low.isInfinite() || low <= 0.0) return "Invalid candle low ($low): must be positive and finite."
        if (close.isNaN() || close.isInfinite() || close <= 0.0) return "Invalid candle close ($close): must be positive and finite."
        if (high < low) return "Corrupted candle: high ($high) is less than low ($low)."
        if (close < low || close > high) return "Out-of-range candle close ($close): must fall within [$low, $high]."
        if (high <= low) return "Zero candle range: high ($high) equals low ($low)."
        return null
    }

    /**
     * Complete ECE score calculation from raw market tick and candle metrics.
     */
    fun analyzeAsset(
        symbol: String,
        assetClass: AssetClass,
        bid: Double,
        ask: Double,
        spread: Double,
        high: Double,
        low: Double,
        close: Double,
        isNewsImminent: Boolean = false,
        spreadPipsOverride: Double? = null,
        newsFeedStatus: NewsFeedStatus? = null,
        timeframeEvidence: MultiTimeframeEvidence? = null
    ): EceAnalysisResult {
        // 1. Strict Input Validation (Prices, bid/ask ordering, candle ranges, NaN, Infinite)
        val validationError = validateMarketInput(bid, ask, spread, high, low, close)
        if (validationError != null) {
            val invalidBreakdown = EceScoreBreakdown(
                overall = 0,
                trendScore = 0,
                structureScore = 0,
                momentumScore = 0,
                confirmationScore = 0,
                timeframeScore = 0,
                setupQuality = 0,
                riskScore = 0,
                newsRiskScore = 0,
                explanation = "DISQUALIFIED: $validationError"
            )
            return EceAnalysisResult(
                symbol = symbol,
                efficiencyScore = 0,
                confidenceScore = 0,
                exposureScore = 0,
                compositeEceScore = 0,
                breakdown = invalidBreakdown,
                status = SetupStatus.INVALIDATED,
                direction = TradeDirection.NONE,
                marketCondition = MarketCondition.RANGING,
                timeframeAlignment = TimeframeAlignment.UNCONFIRMED,
                riskAssessment = "$symbol NONE • DISQUALIFIED: Corrupt or unverified market input data ($validationError)",
                isActionable = false,
                missingEvidenceReason = validationError
            )
        }

        val strategyResult = strategyEngine.evaluateSetup(
            symbol = symbol,
            assetClass = assetClass,
            bid = bid,
            ask = ask,
            spread = spread,
            high = high,
            low = low,
            close = close
        )

        // Derive multi-timeframe alignment from actual evidence if provided
        val actualAlignment = if (timeframeEvidence != null) {
            when {
                !timeframeEvidence.isEvidenceVerified -> TimeframeAlignment.UNCONFIRMED
                timeframeEvidence.h4Trend == timeframeEvidence.h1Trend &&
                        timeframeEvidence.h1Trend == timeframeEvidence.m15Trend &&
                        timeframeEvidence.h4Trend != TradeDirection.NONE -> TimeframeAlignment.FULL_ALIGNMENT
                timeframeEvidence.h4Trend == timeframeEvidence.h1Trend &&
                        timeframeEvidence.h4Trend != TradeDirection.NONE -> TimeframeAlignment.PARTIAL_ALIGNMENT
                timeframeEvidence.h4Trend != timeframeEvidence.h1Trend &&
                        timeframeEvidence.h4Trend != TradeDirection.NONE &&
                        timeframeEvidence.h1Trend != TradeDirection.NONE -> TimeframeAlignment.DIVERGENT
                else -> TimeframeAlignment.UNCONFIRMED
            }
        } else {
            strategyResult.timeframeAlignment
        }

        val spreadPips = spreadPipsOverride ?: calculateSpreadInPips(symbol, spread, assetClass)
        val range = (high - low).coerceAtLeast(0.00001)
        val positionInRange = ((close - low) / range).coerceIn(0.0, 1.0)

        // Effective news feed status: if not explicitly specified, map isNewsImminent
        val resolvedNewsStatus = newsFeedStatus ?: if (isNewsImminent) {
            NewsFeedStatus.IMMINENT_HIGH_IMPACT
        } else {
            NewsFeedStatus.VERIFIED_CLEAR
        }

        val efficiency = calculateEfficiency(positionInRange, spreadPips, range)
        val confidence = calculateConfidence(
            strategyResult.direction,
            actualAlignment,
            strategyResult.setupQualityScore
        )
        val exposure = calculateExposure(spreadPips, isNewsImminent, resolvedNewsStatus)

        val composite = calculateCompositeScore(efficiency, confidence, exposure)

        val hasMissingEvidence = resolvedNewsStatus == NewsFeedStatus.UNVERIFIED_OR_MISSING ||
                actualAlignment == TimeframeAlignment.UNCONFIRMED

        val status = determineSetupStatus(
            preliminary = strategyResult.setupStatus,
            compositeEce = composite,
            exposure = exposure,
            spreadPips = spreadPips,
            hasMissingEvidence = hasMissingEvidence
        )

        val isActionable = status == SetupStatus.QUALIFIED &&
                composite >= QUALIFIED_THRESHOLD &&
                exposure >= MINIMUM_EXPOSURE_QUALIFICATION &&
                !hasMissingEvidence

        val missingReason = when {
            resolvedNewsStatus == NewsFeedStatus.UNVERIFIED_OR_MISSING -> "Macro news feed is unverified or missing; conservative baseline applied."
            actualAlignment == TimeframeAlignment.UNCONFIRMED -> "Multi-timeframe structural alignment unconfirmed across H4/H1/M15."
            spreadPips > MAX_ALLOWED_SPREAD_PIPS -> "Spread ($spreadPips pips) exceeds institutional maximum allowed tolerance ($MAX_ALLOWED_SPREAD_PIPS pips)."
            else -> null
        }

        val breakdown = buildScoreBreakdown(
            composite = composite,
            efficiency = efficiency,
            confidence = confidence,
            exposure = exposure,
            strategyResult = strategyResult,
            spreadPips = spreadPips,
            isNewsImminent = isNewsImminent,
            newsFeedStatus = resolvedNewsStatus,
            timeframeAlignment = actualAlignment
        )

        val riskAssessment = generateRiskAssessmentSummary(
            symbol = symbol,
            composite = composite,
            status = status,
            efficiency = efficiency,
            confidence = confidence,
            exposure = exposure,
            direction = strategyResult.direction,
            isNewsImminent = isNewsImminent,
            spreadPips = spreadPips,
            newsFeedStatus = resolvedNewsStatus,
            missingEvidenceReason = missingReason
        )

        return EceAnalysisResult(
            symbol = symbol,
            efficiencyScore = efficiency,
            confidenceScore = confidence,
            exposureScore = exposure,
            compositeEceScore = composite,
            breakdown = breakdown,
            status = status,
            direction = strategyResult.direction,
            marketCondition = strategyResult.marketCondition,
            timeframeAlignment = actualAlignment,
            riskAssessment = riskAssessment,
            isActionable = isActionable,
            missingEvidenceReason = missingReason
        )
    }

    /**
     * Overload for analyzing stored Room entity data.
     */
    fun analyzeMarketData(
        marketData: MarketDataEntity,
        assetClass: AssetClass = AssetClass.FOREX,
        isNewsImminent: Boolean = false,
        newsFeedStatus: NewsFeedStatus? = null
    ): EceAnalysisResult {
        return analyzeAsset(
            symbol = marketData.symbol,
            assetClass = assetClass,
            bid = marketData.bid,
            ask = marketData.ask,
            spread = marketData.spread,
            high = marketData.high,
            low = marketData.low,
            close = marketData.close,
            isNewsImminent = isNewsImminent,
            newsFeedStatus = newsFeedStatus
        )
    }

    /**
     * Computes ECE analysis from an existing StrategyResult.
     */
    fun evaluateStrategyAndCalculateEce(
        strategyResult: StrategyEngine.StrategyResult,
        spreadPips: Double,
        isNewsImminent: Boolean,
        newsFeedStatus: NewsFeedStatus? = null
    ): EceAnalysisResult {
        val positionInRange = if (strategyResult.direction == TradeDirection.BUY) 0.85 else if (strategyResult.direction == TradeDirection.SELL) 0.15 else 0.50
        val efficiency = calculateEfficiency(positionInRange, spreadPips, 0.0050)
        val confidence = calculateConfidence(
            strategyResult.direction,
            strategyResult.timeframeAlignment,
            strategyResult.setupQualityScore
        )
        val resolvedNewsStatus = newsFeedStatus ?: if (isNewsImminent) NewsFeedStatus.IMMINENT_HIGH_IMPACT else NewsFeedStatus.VERIFIED_CLEAR
        val exposure = calculateExposure(spreadPips, isNewsImminent, resolvedNewsStatus)
        val composite = calculateCompositeScore(efficiency, confidence, exposure)

        val hasMissingEvidence = resolvedNewsStatus == NewsFeedStatus.UNVERIFIED_OR_MISSING ||
                strategyResult.timeframeAlignment == TimeframeAlignment.UNCONFIRMED

        val status = determineSetupStatus(
            preliminary = strategyResult.setupStatus,
            compositeEce = composite,
            exposure = exposure,
            spreadPips = spreadPips,
            hasMissingEvidence = hasMissingEvidence
        )

        val breakdown = buildScoreBreakdown(
            composite = composite,
            efficiency = efficiency,
            confidence = confidence,
            exposure = exposure,
            strategyResult = strategyResult,
            spreadPips = spreadPips,
            isNewsImminent = isNewsImminent,
            newsFeedStatus = resolvedNewsStatus,
            timeframeAlignment = strategyResult.timeframeAlignment
        )

        val missingReason = if (resolvedNewsStatus == NewsFeedStatus.UNVERIFIED_OR_MISSING) {
            "News intelligence feed is unverified or missing."
        } else null

        val riskAssessment = generateRiskAssessmentSummary(
            symbol = strategyResult.symbol,
            composite = composite,
            status = status,
            efficiency = efficiency,
            confidence = confidence,
            exposure = exposure,
            direction = strategyResult.direction,
            isNewsImminent = isNewsImminent,
            spreadPips = spreadPips,
            newsFeedStatus = resolvedNewsStatus,
            missingEvidenceReason = missingReason
        )

        return EceAnalysisResult(
            symbol = strategyResult.symbol,
            efficiencyScore = efficiency,
            confidenceScore = confidence,
            exposureScore = exposure,
            compositeEceScore = composite,
            breakdown = breakdown,
            status = status,
            direction = strategyResult.direction,
            marketCondition = strategyResult.marketCondition,
            timeframeAlignment = strategyResult.timeframeAlignment,
            riskAssessment = riskAssessment,
            isActionable = status == SetupStatus.QUALIFIED && composite >= QUALIFIED_THRESHOLD && exposure >= MINIMUM_EXPOSURE_QUALIFICATION && !hasMissingEvidence,
            missingEvidenceReason = missingReason
        )
    }

    /**
     * Calculates Efficiency (0-100).
     * Measures directional structural displacement minus execution friction / spread penalty.
     */
    fun calculateEfficiency(
        positionInRange: Double,
        spreadPips: Double,
        range: Double
    ): Int {
        // High directional displacement towards candle extreme signals high market efficiency
        val directionalDisplacement = abs(positionInRange - 0.5) * 2.0 // 0.0 (chop) to 1.0 (extreme)
        val baseEfficiency = (55.0 + (directionalDisplacement * 35.0)).coerceIn(40.0, 95.0)

        // Spread friction deduction: High spread relative to market reduces execution efficiency
        val spreadFrictionPenalty = (spreadPips * 1.8).coerceAtMost(25.0)
        return (baseEfficiency - spreadFrictionPenalty).toInt().coerceIn(0, 100)
    }

    /**
     * Calculates Confidence (0-100).
     * Strictly symmetrical: BUY and SELL directional conviction use identical 88.0 weight.
     * Evaluates multi-timeframe structural alignment, directional bias, and setup confluence.
     */
    fun calculateConfidence(
        direction: TradeDirection,
        timeframeAlignment: TimeframeAlignment,
        setupQualityScore: Int
    ): Int {
        if (direction == TradeDirection.NONE) {
            return (setupQualityScore * 0.8).toInt().coerceIn(30, 52)
        }

        // Symmetrical directional conviction: BUY and SELL are treated with equal mathematical conviction
        val directionalConviction = 88.0

        val alignmentScore = when (timeframeAlignment) {
            TimeframeAlignment.FULL_ALIGNMENT -> 92.0
            TimeframeAlignment.PARTIAL_ALIGNMENT -> 70.0
            TimeframeAlignment.DIVERGENT -> 40.0
            TimeframeAlignment.UNCONFIRMED -> 50.0
        }

        val qualityWeight = setupQualityScore.toDouble().coerceIn(50.0, 95.0)

        return ((directionalConviction * 0.35) + (alignmentScore * 0.40) + (qualityWeight * 0.25))
            .toInt()
            .coerceIn(0, 100)
    }

    /**
     * Calculates Exposure (0-100).
     * Higher score = Safer exposure (capital preserved, low adverse event risk).
     * Lower score = High risk / dangerous exposure (imminent news event, wide spread, or unverified feeds).
     *
     * Rule: Missing news data must NOT be treated as confirmed low risk. When unverified, a conservative
     * baseline (55) is applied to prevent false low-risk qualification.
     */
    fun calculateExposure(
        spreadPips: Double,
        isNewsImminent: Boolean,
        newsFeedStatus: NewsFeedStatus = if (isNewsImminent) NewsFeedStatus.IMMINENT_HIGH_IMPACT else NewsFeedStatus.VERIFIED_CLEAR
    ): Int {
        val baseSafety = when (newsFeedStatus) {
            NewsFeedStatus.VERIFIED_CLEAR -> 88
            NewsFeedStatus.IMMINENT_HIGH_IMPACT -> 43 // Penalized due to confirmed macro event
            NewsFeedStatus.UNVERIFIED_OR_MISSING -> 55 // Conservative baseline: unverified is NOT low risk
        }

        val spreadPenalty = (spreadPips * 1.5).toInt().coerceAtMost(18)

        return (baseSafety - spreadPenalty).coerceIn(10, 100)
    }

    /**
     * Computes the weighted Composite ECE score (0-100).
     */
    fun calculateCompositeScore(
        efficiency: Int,
        confidence: Int,
        exposure: Int
    ): Int {
        val weighted = (efficiency * WEIGHT_EFFICIENCY) +
                (confidence * WEIGHT_CONFIDENCE) +
                (exposure * WEIGHT_EXPOSURE)
        return weighted.toInt().coerceIn(0, 100)
    }

    /**
     * Strictly defines the precise meanings of QUALIFIED, MONITORING, and INVALIDATED:
     * - QUALIFIED: ECE >= 70, confirmed directional setup, exposure >= 60, spread <= 6.0 pips, verified inputs.
     * - MONITORING: ECE 50-69, or ECE >= 70 with cautionary factors (e.g., unverified news, partial alignment).
     * - INVALIDATED: ECE < 50, corrupt data, excessive spread (> 6.0 pips), or neutral/opposing regime.
     */
    private fun determineSetupStatus(
        preliminary: SetupStatus,
        compositeEce: Int,
        exposure: Int,
        spreadPips: Double,
        hasMissingEvidence: Boolean
    ): SetupStatus {
        return when {
            spreadPips > MAX_ALLOWED_SPREAD_PIPS -> SetupStatus.INVALIDATED
            compositeEce < MONITORING_THRESHOLD -> SetupStatus.INVALIDATED
            preliminary == SetupStatus.QUALIFIED &&
                    compositeEce >= QUALIFIED_THRESHOLD &&
                    exposure >= MINIMUM_EXPOSURE_QUALIFICATION &&
                    !hasMissingEvidence -> SetupStatus.QUALIFIED
            compositeEce >= MONITORING_THRESHOLD -> SetupStatus.MONITORING
            else -> SetupStatus.INVALIDATED
        }
    }

    private fun buildScoreBreakdown(
        composite: Int,
        efficiency: Int,
        confidence: Int,
        exposure: Int,
        strategyResult: StrategyEngine.StrategyResult,
        spreadPips: Double,
        isNewsImminent: Boolean,
        newsFeedStatus: NewsFeedStatus,
        timeframeAlignment: TimeframeAlignment
    ): EceScoreBreakdown {
        // Symmetrical trend score for both BUY and SELL
        val trendScore = if (strategyResult.direction != TradeDirection.NONE) 88 else 50
        val structureScore = efficiency
        val momentumScore = ((confidence + efficiency) / 2).coerceIn(40, 95)
        val confirmationScore = confidence
        val timeframeScore = when (timeframeAlignment) {
            TimeframeAlignment.FULL_ALIGNMENT -> 90
            TimeframeAlignment.PARTIAL_ALIGNMENT -> 70
            TimeframeAlignment.DIVERGENT -> 40
            TimeframeAlignment.UNCONFIRMED -> 50
        }

        val newsRiskScore = when (newsFeedStatus) {
            NewsFeedStatus.VERIFIED_CLEAR -> 90
            NewsFeedStatus.IMMINENT_HIGH_IMPACT -> 35
            NewsFeedStatus.UNVERIFIED_OR_MISSING -> 50
        }

        val explanation = "ECE $composite/100: Efficiency ($efficiency), Confidence ($confidence), Exposure ($exposure). " +
                if (composite >= QUALIFIED_THRESHOLD && exposure >= MINIMUM_EXPOSURE_QUALIFICATION && newsFeedStatus == NewsFeedStatus.VERIFIED_CLEAR) {
                    "Statistical edge verified under institutional risk parameters."
                } else if (newsFeedStatus == NewsFeedStatus.UNVERIFIED_OR_MISSING) {
                    "Setup sub-threshold or unverified news input; monitoring without capital commitment."
                } else {
                    "Setup sub-threshold; monitoring without capital commitment."
                }

        return EceScoreBreakdown(
            overall = composite,
            trendScore = trendScore,
            structureScore = structureScore,
            momentumScore = momentumScore,
            confirmationScore = confirmationScore,
            timeframeScore = timeframeScore,
            setupQuality = strategyResult.setupQualityScore,
            riskScore = exposure,
            newsRiskScore = newsRiskScore,
            explanation = explanation
        )
    }

    private fun generateRiskAssessmentSummary(
        symbol: String,
        composite: Int,
        status: SetupStatus,
        efficiency: Int,
        confidence: Int,
        exposure: Int,
        direction: TradeDirection,
        isNewsImminent: Boolean,
        spreadPips: Double,
        newsFeedStatus: NewsFeedStatus,
        missingEvidenceReason: String?
    ): String {
        val actionText = when (status) {
            SetupStatus.QUALIFIED -> "ACTIONABLE: Verified ECE qualification ($composite/100). Allocation capped at 1 active trade."
            SetupStatus.MONITORING -> "STANDBY: ECE score $composite/100 is below the institutional 70-point execution threshold."
            SetupStatus.INVALIDATED -> "DISQUALIFIED: ECE score $composite/100 indicates unfavorable market condition."
            SetupStatus.NO_SETUP -> "NO SETUP: Inactive regime."
        }
        val newsWarning = when (newsFeedStatus) {
            NewsFeedStatus.IMMINENT_HIGH_IMPACT -> " [WARNING: High-impact macro news window active]"
            NewsFeedStatus.UNVERIFIED_OR_MISSING -> " [CAUTION: News feed unverified / missing]"
            NewsFeedStatus.VERIFIED_CLEAR -> ""
        }
        val spreadNote = if (spreadPips > MAX_ALLOWED_SPREAD_PIPS) " [ALERT: Elevated spread friction (${spreadPips} pips)]" else ""
        val missingNote = if (missingEvidenceReason != null) " [MISSING EVIDENCE: $missingEvidenceReason]" else ""

        return "$symbol $direction • $actionText (E:$efficiency C:$confidence Exp:$exposure)$newsWarning$spreadNote$missingNote"
    }

    /**
     * Asset-class aware spread calculation adhering to precision units:
     * - Forex: Standard 4/5-digit pairs use 10,000 multiplier; JPY pairs use 100 multiplier.
     * - Metals: Gold (XAUUSD) uses 10 multiplier (0.01 precision); Silver (XAGUSD) uses 100 multiplier.
     * - Indices: Direct index points (e.g. US30 1.0 point, NAS100 1.5 points).
     * - Crypto: Direct USD points (e.g. BTCUSD 3.2 USD).
     */
    fun calculateSpreadInPips(symbol: String, rawSpread: Double, assetClass: AssetClass): Double {
        if (rawSpread.isNaN() || rawSpread.isInfinite() || rawSpread < 0.0) return 1.5
        val safeSpread = rawSpread.coerceAtLeast(0.0)
        return when (assetClass) {
            AssetClass.INDICES -> safeSpread.coerceIn(0.1, 50.0)
            AssetClass.CRYPTO -> safeSpread.coerceIn(0.1, 100.0)
            AssetClass.METALS -> {
                if (symbol.contains("XAG", ignoreCase = true)) {
                    (safeSpread * 100.0).coerceIn(0.1, 50.0)
                } else {
                    (safeSpread * 10.0).coerceIn(0.1, 50.0)
                }
            }
            AssetClass.FOREX -> {
                if (symbol.contains("JPY", ignoreCase = true)) {
                    (safeSpread * 100.0).coerceIn(0.1, 20.0)
                } else {
                    (safeSpread * 10000.0).coerceIn(0.1, 20.0)
                }
            }
        }
    }
}
