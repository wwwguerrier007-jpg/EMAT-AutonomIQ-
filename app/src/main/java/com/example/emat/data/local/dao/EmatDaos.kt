package com.example.emat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.emat.data.local.entity.AccountEntitlementEntity
import com.example.emat.data.local.entity.AuditLogEntity
import com.example.emat.data.local.entity.ConfigEntryEntity
import com.example.emat.data.local.entity.EceScoreEntity
import com.example.emat.data.local.entity.EvidenceEntity
import com.example.emat.data.local.entity.InstrumentEntity
import com.example.emat.data.local.entity.MarketDataEntity
import com.example.emat.data.local.entity.OpportunityEntity
import com.example.emat.data.local.entity.RiskCheckEntity
import com.example.emat.data.local.entity.ScannerRunEntity
import com.example.emat.data.local.entity.SystemHealthEntity
import com.example.emat.data.local.entity.TradeEntity
import com.example.emat.data.local.entity.UserSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstrumentDao {
    @Query("SELECT * FROM instruments ORDER BY assetClass ASC, symbol ASC")
    fun getAllInstruments(): Flow<List<InstrumentEntity>>

    @Query("SELECT * FROM instruments WHERE isActive = 1")
    suspend fun getActiveInstruments(): List<InstrumentEntity>

    @Query("SELECT * FROM instruments WHERE symbol = :symbol")
    suspend fun getInstrumentBySymbol(symbol: String): InstrumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstruments(instruments: List<InstrumentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstrument(instrument: InstrumentEntity)

    @Update
    suspend fun updateInstrument(instrument: InstrumentEntity)

    @Query("DELETE FROM instruments WHERE symbol = :symbol")
    suspend fun deleteInstrument(symbol: String)

    @Query("UPDATE instruments SET isActive = :isActive WHERE symbol = :symbol")
    suspend fun setInstrumentActive(symbol: String, isActive: Boolean)
}

@Dao
interface MarketDataDao {
    @Query("SELECT * FROM market_data")
    fun getAllMarketDataFlow(): Flow<List<MarketDataEntity>>

    @Query("SELECT * FROM market_data WHERE symbol = :symbol")
    suspend fun getMarketData(symbol: String): MarketDataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketData(data: List<MarketDataEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleMarketData(data: MarketDataEntity)
}

@Dao
interface ScannerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScannerRun(run: ScannerRunEntity)

    @Query("SELECT * FROM scanner_runs ORDER BY timestamp DESC LIMIT 1")
    fun getLatestScannerRun(): Flow<ScannerRunEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpportunities(opps: List<OpportunityEntity>)

    @Query("SELECT * FROM opportunities ORDER BY timestamp DESC, rank ASC LIMIT 3")
    fun getTopOpportunities(): Flow<List<OpportunityEntity>>

    @Query("DELETE FROM opportunities")
    suspend fun clearOpportunities()
}

@Dao
interface OpportunityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpportunities(opps: List<OpportunityEntity>)

    @Query("SELECT * FROM opportunities ORDER BY timestamp DESC, rank ASC LIMIT 3")
    fun getTopOpportunities(): Flow<List<OpportunityEntity>>

    @Query("DELETE FROM opportunities")
    suspend fun clearOpportunities()
}

@Dao
interface EceScoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: EceScoreEntity)

    @Query("SELECT * FROM ece_scores WHERE symbol = :symbol ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestScore(symbol: String): EceScoreEntity?

    @Query("SELECT * FROM ece_scores ORDER BY timestamp DESC LIMIT 20")
    fun getRecentScores(): Flow<List<EceScoreEntity>>
}

@Dao
interface RiskCheckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRiskCheck(check: RiskCheckEntity)

    @Query("SELECT * FROM risk_checks ORDER BY timestamp DESC LIMIT 20")
    fun getRecentRiskChecks(): Flow<List<RiskCheckEntity>>
}

@Dao
interface TradeDao {
    @Query("SELECT * FROM trades WHERE status = 'ACTIVE' LIMIT 1")
    fun getActiveTradeFlow(): Flow<TradeEntity?>

    @Query("SELECT * FROM trades WHERE accountId = :accountId AND status = 'ACTIVE' LIMIT 1")
    fun getActiveTradeByAccountFlow(accountId: String): Flow<TradeEntity?>

    @Query("SELECT COUNT(*) FROM trades WHERE status = 'ACTIVE'")
    suspend fun getActiveTradeCount(): Int

    @Query("SELECT COUNT(*) FROM trades WHERE accountId = :accountId AND status = 'ACTIVE'")
    suspend fun getActiveTradeCountByAccount(accountId: String): Int

    @Query("SELECT * FROM trades ORDER BY openedAt DESC")
    fun getAllTrades(): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE accountId = :accountId ORDER BY openedAt DESC")
    fun getTradesByAccountFlow(accountId: String): Flow<List<TradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeEntity)

    @Update
    suspend fun updateTrade(trade: TradeEntity)

    @Query("UPDATE trades SET status = 'EMERGENCY_STOPPED' WHERE status = 'ACTIVE'")
    suspend fun emergencyStopAllTrades()
}

@Dao
interface EvidenceDao {
    @Query("SELECT * FROM evidence_records WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getEvidenceByAccount(accountId: String): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence_records ORDER BY timestamp DESC LIMIT 50")
    fun getAllEvidence(): Flow<List<EvidenceEntity>>

    // Append-only: No update and no delete queries allowed
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidence(evidence: EvidenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEvidence(evidenceList: List<EvidenceEntity>)
}

@Dao
interface SystemHealthDao {
    @Query("SELECT * FROM system_health")
    fun getAllHealthFlow(): Flow<List<SystemHealthEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHealth(health: SystemHealthEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHealth(healthList: List<SystemHealthEntity>)
}

@Dao
interface UserSessionDao {
    @Query("SELECT * FROM user_sessions WHERE id = 1")
    fun getSessionFlow(): Flow<UserSessionEntity?>

    @Query("SELECT * FROM user_sessions WHERE id = 1")
    suspend fun getSession(): UserSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: UserSessionEntity)

    @Query("UPDATE user_sessions SET isLoggedIn = 0, sessionToken = NULL WHERE id = 1")
    suspend fun clearSession()
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM configuration WHERE `key` = :key")
    suspend fun getConfig(key: String): ConfigEntryEntity?

    @Query("SELECT * FROM configuration")
    fun getAllConfigs(): Flow<List<ConfigEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfig(entry: ConfigEntryEntity)
}

@Dao
interface EntitlementDao {
    @Query("SELECT * FROM account_entitlements WHERE accountId = :accountId")
    fun getEntitlementFlow(accountId: String): Flow<AccountEntitlementEntity?>

    @Query("SELECT * FROM account_entitlements WHERE accountId = :accountId")
    suspend fun getEntitlement(accountId: String): AccountEntitlementEntity?

    @Query("SELECT * FROM account_entitlements")
    fun getAllEntitlementsFlow(): Flow<List<AccountEntitlementEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntitlement(entitlement: AccountEntitlementEntity)

    @Update
    suspend fun updateEntitlement(entitlement: AccountEntitlementEntity)

    @Query("DELETE FROM account_entitlements WHERE accountId = :accountId")
    suspend fun deleteEntitlement(accountId: String)
}
