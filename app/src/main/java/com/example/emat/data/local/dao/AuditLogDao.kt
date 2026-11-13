package com.example.emat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.emat.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for security and operational audit logging.
 *
 * Provides append-only insertions and queries for security-sensitive events.
 * Sensitive fields (passwords, PINs, recovery codes) are strictly prohibited
 * and excluded from the underlying [AuditLogEntity] schema.
 */
@Dao
interface AuditLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(logs: List<AuditLogEntity>)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE accountId = :accountId ORDER BY timestamp DESC LIMIT 100")
    fun getRecentAuditLogsByAccount(accountId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE category = :category ORDER BY timestamp DESC LIMIT 50")
    fun getAuditLogsByCategory(category: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE category IN ('AUTH', 'SECURITY') ORDER BY timestamp DESC LIMIT 100")
    fun getSecurityAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getAuditLogsSince(sinceTimestamp: Long): Flow<List<AuditLogEntity>>

    @Query("SELECT COUNT(*) FROM audit_logs")
    suspend fun getTotalLogCount(): Int

    @Query("SELECT COUNT(*) FROM audit_logs WHERE category = :category")
    suspend fun getLogCountByCategory(category: String): Int
}
