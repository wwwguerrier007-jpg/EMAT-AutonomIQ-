package com.example.emat.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.security.MessageDigest

@Entity(tableName = "instruments")
data class InstrumentEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val assetClass: String,
    val isActive: Boolean = true,
    val pipSize: Double,
    val digits: Int,
    val minVolume: Double = 0.01,
    val contractSize: Double = 100000.0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "market_data")
data class MarketDataEntity(
    @PrimaryKey val symbol: String,
    val bid: Double,
    val ask: Double,
    val spread: Double,
    val timeframe: String = "H1",
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val dataProvider: String, // "REAL", "TEST", "DEMO", "NOT_CONNECTED"
    val timestamp: Long,
    val isStale: Boolean = false
)

@Entity(tableName = "scanner_runs")
data class ScannerRunEntity(
    @PrimaryKey val runId: String,
    val timestamp: Long,
    val instrumentsScanned: Int,
    val instrumentsQualifying: Int,
    val status: String,
    val durationMs: Long
)

@Entity(tableName = "opportunities")
data class OpportunityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanRunId: String,
    val rank: Int,
    val symbol: String,
    val assetClass: String,
    val direction: String, // "BUY", "SELL", "NONE"
    val eceScore: Int,
    val marketCondition: String,
    val setupStatus: String,
    val timeframeAlignment: String,
    val riskStatus: String,
    val whyItQualifies: String,
    val timestamp: Long,
    val freshnessSeconds: Long
)

@Entity(tableName = "ece_scores")
data class EceScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val overallEce: Int,
    val trendScore: Int,
    val structureScore: Int,
    val momentumScore: Int,
    val confirmationScore: Int,
    val timeframeScore: Int,
    val setupQuality: Int,
    val riskScore: Int,
    val newsRiskScore: Int,
    val explanation: String,
    val timestamp: Long
)

@Entity(tableName = "risk_checks")
data class RiskCheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val decision: String, // "APPROVED", "REJECTED"
    val accountRiskPercent: Double,
    val positionSize: Double,
    val stopLossDistancePips: Double,
    val spreadPips: Double,
    val activeTradesCount: Int,
    val maxAllowedTrades: Int = 1,
    val rejectionReason: String?,
    val timestamp: Long
)

@Entity(tableName = "trades")
data class TradeEntity(
    @PrimaryKey val tradeId: String,
    val accountId: String,
    val symbol: String,
    val direction: String,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val positionSize: Double,
    val currentPrice: Double,
    val unrealizedResult: Double,
    val isBreakEven: Boolean,
    val riskStatus: String,
    val status: String, // "PENDING", "ACTIVE", "CLOSED", "EMERGENCY_STOPPED"
    val executionMode: String, // "NOT_CONNECTED", "DEMO", "LIVE"
    val openedAt: Long,
    val closedAt: Long? = null
)

@Entity(tableName = "evidence_records")
data class EvidenceEntity(
    @PrimaryKey val evidenceId: String,
    val accountId: String,
    val instrument: String,
    val timeframe: String,
    val sourceRef: String,
    val timestamp: Long,
    val observation: String,
    val calculation: String,
    val interpretation: String,
    val relatedOpportunityId: String? = null,
    val relatedTradeId: String? = null,
    val verificationStatus: String = "AUDITED_QUALIFIED"
)

@Entity(tableName = "system_health")
data class SystemHealthEntity(
    @PrimaryKey val componentKey: String,
    val status: String, // "HEALTHY", "WARNING", "ERROR", "NOT_CONNECTED"
    val lastCheckTime: Long,
    val details: String,
    val errorCount: Int = 0
)

@Entity(tableName = "user_sessions")
data class UserSessionEntity(
    @PrimaryKey val id: Int = 1,
    val isLoggedIn: Boolean,
    val username: String,
    val sessionToken: String?,
    val lastActiveTime: Long,
    val authMethod: String
)

@Entity(tableName = "configuration")
data class ConfigEntryEntity(
    @PrimaryKey val key: String,
    val value: String,
    val description: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "account_entitlements")
data class AccountEntitlementEntity(
    @PrimaryKey val accountId: String,
    val trialStartedAt: Long?,
    val trialDurationDays: Int,
    val accumulatedTrialDurationMillis: Long,
    val lastObservedTimestamp: Long,
    val hasClockAnomaly: Boolean,
    val subscriptionId: String?,
    val subscriptionPlanId: String?,
    val subscriptionPurchasedAt: Long?,
    val subscriptionExpiresAt: Long?,
    val verifiedByBillingProvider: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)
