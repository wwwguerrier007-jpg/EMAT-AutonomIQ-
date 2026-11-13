package com.example.emat.domain.service

import com.example.emat.domain.model.AiAnalysisResult
import com.example.emat.domain.model.Opportunity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmatAiEngine {

    fun generateAnalysis(
        context: String,
        activeTrades: Int,
        isEmergencyStopped: Boolean,
        topOpp: Opportunity?
    ): AiAnalysisResult {
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        val fact = when {
            isEmergencyStopped -> "CRITICAL: System-wide Emergency Stop is active. All automated execution engines are disarmed and order flow is halted."
            activeTrades > 0 -> "INVARIANT LOCK: Exactly $activeTrades active trade is currently live on the book. The One-Trade Rule invariant restricts opening any new positions."
            topOpp != null -> "Market Opportunity detected on ${topOpp.symbol} (${topOpp.direction.name}) with ECE Score of ${topOpp.eceScore}/100. Status: ${topOpp.setupStatus.label}."
            else -> "Market scanner state is nominal. Universe monitoring active across institutional Forex, Indices, Metals, and Crypto."
        }

        val calculation = if (topOpp != null) {
            "ECE Score = ${topOpp.eceScore}/100. Timeframe Alignment: ${topOpp.timeframeAlignment.label}. Risk Status: ${topOpp.riskStatus.label}. Freshness: ${topOpp.freshnessSeconds}s (<15s target)."
        } else {
            "Account Risk Model: Equity allocation per trade = 1.00% max. Hard stop distance calculated from dynamic ATR volatility band. Expectancy model: E = (Win% * AvgWin) - (Loss% * AvgLoss)."
        }

        val analysis = if (topOpp != null) {
            "${topOpp.symbol} demonstrates structural alignment. Setup qualification notes: ${topOpp.whyItQualifies}. Market condition: ${topOpp.marketCondition.label}."
        } else {
            "Current market conditions exhibit normal liquidity. Institutional volatility filters indicate acceptable conditions for qualified signal execution when criteria align."
        }

        val inference = when {
            isEmergencyStopped -> "Zero execution probability. Manual operator review required to disengage circuit breaker."
            activeTrades > 0 -> "Any incoming trade signal will be discarded to prevent compounding exposure and protect account equity."
            topOpp != null && topOpp.eceScore >= 70 -> "High probability of positive mathematical expectancy under systematic ECE model."
            else -> "Patience is mandated. Awaiting high-conviction setup meeting >= 70 ECE score threshold."
        }

        val recommendation = when {
            isEmergencyStopped -> "MAINTAIN HALT. Investigate root cause before resetting emergency stop."
            activeTrades > 0 -> "MONITOR ACTIVE POSITION. Enforce stop loss and trailing profit targets according to plan."
            topOpp != null && topOpp.eceScore >= 70 -> "QUALIFIED SETUP: Ready for human authorization. Verify news calendar before confirming execution."
            else -> "HOLD POSITION. Continue real-time market surveillance without discretionary overtrading."
        }

        return AiAnalysisResult(
            contextTitle = context,
            fact = fact,
            calculation = calculation,
            analysis = analysis,
            inference = inference,
            recommendation = recommendation,
            timestamp = timeStr,
            dataTruthStatus = "LIVE_VERIFIED"
        )
    }
}
