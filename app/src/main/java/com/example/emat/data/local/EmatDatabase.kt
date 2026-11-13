package com.example.emat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.emat.data.local.dao.AuditLogDao
import com.example.emat.data.local.dao.ConfigDao
import com.example.emat.data.local.dao.EceScoreDao
import com.example.emat.data.local.dao.EvidenceDao
import com.example.emat.data.local.dao.InstrumentDao
import com.example.emat.data.local.dao.MarketDataDao
import com.example.emat.data.local.dao.OpportunityDao
import com.example.emat.data.local.dao.RiskCheckDao
import com.example.emat.data.local.dao.ScannerDao
import com.example.emat.data.local.dao.SystemHealthDao
import com.example.emat.data.local.dao.TradeDao
import com.example.emat.data.local.dao.UserSessionDao
import com.example.emat.data.local.entity.AuditLogEntity
import com.example.emat.data.local.entity.ConfigEntryEntity
import com.example.emat.data.local.entity.EceScoreEntity
import com.example.emat.data.local.entity.EvidenceEntity
import com.example.emat.data.local.entity.InstrumentEntity
import com.example.emat.data.local.entity.MarketDataEntity
import com.example.emat.data.local.entity.OpportunityEntity
import com.example.emat.data.local.entity.RiskCheckEntity
import com.example.emat.data.local.entity.ScannerRunEntity
import com.example.emat.data.local.dao.EntitlementDao
import com.example.emat.data.local.entity.AccountEntitlementEntity
import com.example.emat.data.local.entity.SystemHealthEntity
import com.example.emat.data.local.entity.TradeEntity
import com.example.emat.data.local.entity.UserSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        InstrumentEntity::class,
        MarketDataEntity::class,
        ScannerRunEntity::class,
        OpportunityEntity::class,
        EceScoreEntity::class,
        RiskCheckEntity::class,
        TradeEntity::class,
        AuditLogEntity::class,
        EvidenceEntity::class,
        SystemHealthEntity::class,
        UserSessionEntity::class,
        ConfigEntryEntity::class,
        AccountEntitlementEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class EmatDatabase : RoomDatabase() {
    abstract fun instrumentDao(): InstrumentDao
    abstract fun marketDataDao(): MarketDataDao
    abstract fun scannerDao(): ScannerDao
    abstract fun eceScoreDao(): EceScoreDao
    abstract fun riskCheckDao(): RiskCheckDao
    abstract fun tradeDao(): TradeDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun systemHealthDao(): SystemHealthDao
    abstract fun userSessionDao(): UserSessionDao
    abstract fun configDao(): ConfigDao
    abstract fun entitlementDao(): EntitlementDao

    companion object {
        @Volatile
        private var INSTANCE: EmatDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS user_sessions (id INTEGER NOT NULL PRIMARY KEY, isLoggedIn INTEGER NOT NULL, username TEXT NOT NULL, sessionToken TEXT, lastActiveTime INTEGER NOT NULL, authMethod TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS configuration (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL, description TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE audit_logs ADD COLUMN eventMetadata TEXT NOT NULL DEFAULT '{}'")
                db.execSQL("ALTER TABLE audit_logs ADD COLUMN integrityHash TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_accountId_timestamp ON audit_logs(accountId, timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_category_action ON audit_logs(category, action)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `account_entitlements` (
                        `accountId` TEXT NOT NULL,
                        `trialStartedAt` INTEGER,
                        `trialDurationDays` INTEGER NOT NULL,
                        `accumulatedTrialDurationMillis` INTEGER NOT NULL,
                        `lastObservedTimestamp` INTEGER NOT NULL,
                        `hasClockAnomaly` INTEGER NOT NULL,
                        `subscriptionId` TEXT,
                        `subscriptionPlanId` TEXT,
                        `subscriptionPurchasedAt` INTEGER,
                        `subscriptionExpiresAt` INTEGER,
                        `verifiedByBillingProvider` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`accountId`)
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): EmatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EmatDatabase::class.java,
                    "emat_autonomiq.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialUniverse(getDatabase(context))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateInitialUniverse(db: EmatDatabase) {
            val initialInstruments = listOf(
                // Forex
                InstrumentEntity("EURUSD", "Euro / US Dollar", "FOREX", true, 0.0001, 5, 0.01, 100000.0),
                InstrumentEntity("GBPUSD", "British Pound / US Dollar", "FOREX", true, 0.0001, 5, 0.01, 100000.0),
                InstrumentEntity("USDJPY", "US Dollar / Japanese Yen", "FOREX", true, 0.01, 3, 0.01, 100000.0),
                InstrumentEntity("USDCHF", "US Dollar / Swiss Franc", "FOREX", true, 0.0001, 5, 0.01, 100000.0),
                InstrumentEntity("USDCAD", "US Dollar / Canadian Dollar", "FOREX", true, 0.0001, 5, 0.01, 100000.0),
                InstrumentEntity("AUDUSD", "Australian Dollar / US Dollar", "FOREX", true, 0.0001, 5, 0.01, 100000.0),
                InstrumentEntity("NZDUSD", "New Zealand Dollar / US Dollar", "FOREX", true, 0.0001, 5, 0.01, 100000.0),
                InstrumentEntity("EURGBP", "Euro / British Pound", "FOREX", true, 0.0001, 5, 0.01, 100000.0),
                InstrumentEntity("EURJPY", "Euro / Japanese Yen", "FOREX", true, 0.01, 3, 0.01, 100000.0),
                InstrumentEntity("GBPJPY", "British Pound / Japanese Yen", "FOREX", true, 0.01, 3, 0.01, 100000.0),

                // Metals
                InstrumentEntity("XAUUSD", "Gold / US Dollar", "METALS", true, 0.01, 2, 0.01, 100.0),
                InstrumentEntity("XAGUSD", "Silver / US Dollar", "METALS", true, 0.001, 3, 0.01, 5000.0),

                // Indices
                InstrumentEntity("NAS100", "Nasdaq 100 Index", "INDICES", true, 0.1, 2, 0.1, 1.0),
                InstrumentEntity("US500", "S&P 500 Index", "INDICES", true, 0.1, 2, 0.1, 1.0),
                InstrumentEntity("US30", "Dow Jones 30 Index", "INDICES", true, 1.0, 1, 0.1, 1.0),
                InstrumentEntity("GER40", "DAX 40 Index", "INDICES", true, 0.1, 2, 0.1, 1.0),
                InstrumentEntity("UK100", "FTSE 100 Index", "INDICES", true, 0.1, 2, 0.1, 1.0),

                // Crypto
                InstrumentEntity("BTCUSD", "Bitcoin / US Dollar", "CRYPTO", true, 0.1, 2, 0.01, 1.0),
                InstrumentEntity("ETHUSD", "Ethereum / US Dollar", "CRYPTO", true, 0.01, 2, 0.01, 1.0)
            )
            db.instrumentDao().insertInstruments(initialInstruments)

            // Initial System Health records
            val healthItems = listOf(
                SystemHealthEntity("APPLICATION", "HEALTHY", System.currentTimeMillis(), "Core shell initialized"),
                SystemHealthEntity("DATABASE", "HEALTHY", System.currentTimeMillis(), "Room local persistence active"),
                SystemHealthEntity("MARKET_DATA", "NOT_CONNECTED", System.currentTimeMillis(), "Market data provider in NOT_CONNECTED state"),
                SystemHealthEntity("SCANNER", "HEALTHY", System.currentTimeMillis(), "Full universe scanner operational"),
                SystemHealthEntity("AUTH", "HEALTHY", System.currentTimeMillis(), "Salted cryptographic verification active"),
                SystemHealthEntity("NEWS_DATA", "NOT_CONNECTED", System.currentTimeMillis(), "Awaiting institutional feed link"),
                SystemHealthEntity("EXECUTION", "NOT_CONNECTED", System.currentTimeMillis(), "Broker adapter in NOT_CONNECTED state")
            )
            db.systemHealthDao().insertAllHealth(healthItems)

            // Initial Configs
            db.configDao().setConfig(ConfigEntryEntity("MAX_ACTIVE_TRADES", "1", "One-Trade Rule enforced"))
            db.configDao().setConfig(ConfigEntryEntity("DATA_STALE_THRESHOLD_SEC", "15", "Seconds before tick marked stale"))
            db.configDao().setConfig(ConfigEntryEntity("MIN_ECE_QUALIFICATION", "70", "Minimum ECE score to qualify setup"))
            db.configDao().setConfig(ConfigEntryEntity("MT5_STATUS", "NOT_CONNECTED", "MetaTrader 5 adapter connection state"))

            // Initial audit log
            val initLog = AuditLogEntity.createSecureEntry(
                accountId = "SYSTEM",
                category = "SYSTEM",
                action = "INITIALIZE_PLATFORM",
                actor = "SYSTEM",
                rationale = "EMAT AutonomIQ platform initialized",
                dataAvailable = "19 instruments seeded across Forex, Metals, Indices, Crypto",
                decisionMade = "Configured fail-safe states and One-Trade rule",
                outcome = "Ready for secure session",
                timestamp = System.currentTimeMillis()
            )
            db.auditLogDao().insertAuditLog(initLog)

            // Initial baseline evidence
            db.evidenceDao().insertEvidence(
                EvidenceEntity(
                    evidenceId = "EVD-SYS-INIT-001",
                    accountId = "SYSTEM",
                    instrument = "XAUUSD",
                    timeframe = "H1",
                    sourceRef = "SYSTEM_INITIALIZATION",
                    timestamp = System.currentTimeMillis(),
                    observation = "Institutional universe configured across 19 instruments (Forex, Metals, Indices, Crypto).",
                    calculation = "1-Trade Rule invariant enforced (maxActiveTrades = 1).",
                    interpretation = "Fail-safe baseline operational. Awaiting market scan execution.",
                    verificationStatus = "AUDITED_QUALIFIED"
                )
            )
        }
    }
}
