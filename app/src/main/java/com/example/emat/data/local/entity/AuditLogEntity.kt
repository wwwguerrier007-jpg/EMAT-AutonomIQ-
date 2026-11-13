package com.example.emat.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.security.MessageDigest

/**
 * Strict Security Policy and Allowlists for Institutional Audit Logging.
 * Enforces zero-leakage of credentials and prevents unapproved data injection.
 */
object AuditSecurityPolicy {
    val PERMITTED_CATEGORIES = setOf(
        "AUTH", "SECURITY", "COMMISSIONING", "RECOVERY",
        "SCANNER", "DATA", "STRATEGY", "RISK",
        "EXECUTION", "SYSTEM", "EMERGENCY", "ENTITLEMENT"
    )

    val PERMITTED_ACTIONS = setOf(
        "FIRST_OPERATOR_COMMISSIONED", "LOGIN_SUCCESS", "LOGIN_FAILED", "LOGOUT",
        "SESSION_RESTORED", "CREDENTIAL_RECOVERED", "RECOVERY_FAILED",
        "UNAUTHORIZED_OPERATION_BLOCKED", "SCAN_EXECUTED", "SCAN_PREVENTED",
        "UNIVERSE_SCAN_COMPLETED", "SCAN_ABORTED", "SCAN_BLOCKED_DATA_NOT_CONNECTED",
        "MARKET_DATA_STATE_CHANGED", "REGISTRY_ADD_INSTRUMENT", "REGISTRY_REMOVE_INSTRUMENT",
        "REGISTRY_UPDATE_INSTRUMENT", "CONFIG_INSTRUMENT", "INSTRUMENT_ACTIVATED",
        "INSTRUMENT_DEACTIVATED", "INSTRUMENT_ADDED", "EMERGENCY_STOP_TRIGGERED",
        "EMERGENCY_STOP_RESET", "SYSTEM_HEALTH_UPDATED", "ACCOUNT_SWITCHED",
        "CONFIG_UPDATED", "TRADE_ORDER_VALIDATED", "TRADE_ORDER_EXECUTED",
        "TRADE_ORDER_REJECTED", "TRADE_CLOSED", "RISK_LIMIT_ENFORCED",
        "COMMISSIONING_TEST", "BIOMETRIC_LOGIN_SUCCESS", "SECURITY_CHECK_1", "SECURITY_CHECK_2", "SECURITY_CHECK",
        "TRIAL_STARTED", "TRIAL_EXPIRED", "ENTITLEMENT_CHANGED", "CLOCK_ANOMALY_DETECTED",
        "FEATURE_ACCESS_DENIED", "MARKET_DATA_STATE_CHANGED"
    )

    val PERMITTED_METADATA_KEYS = setOf(
        "instrument", "symbol", "direction", "eceScore", "positionSize",
        "accountRiskPercent", "feedType", "durationMs", "scannedCount",
        "qualifyingCount", "actorId", "authMethod", "reason", "errorType",
        "attemptCount", "timeframe", "executionState", "marketDataStatus",
        "statusCode", "component", "spreadPips"
    )

    fun isCategoryPermitted(category: String): Boolean = category.uppercase() in PERMITTED_CATEGORIES
    fun isActionPermitted(action: String): Boolean = action in PERMITTED_ACTIONS

    private val RECOVERY_CODE_PATTERN = Regex("""(?i)\b(EMAT-RC-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4})\b""")
    private val GENERIC_KEY_VALUE_SECRET = Regex("""(?i)\b(password|passcode|secret|recovery[_\s-]?code|token|key|private[_\s-]?key)\s*[:=]\s*([^\s,;]+)""")
    private val PHC_HASH_PATTERN = Regex("\\\$pbkdf2-sha256\\\$[^\\s]+")
    private val RAW_SALT_VERIFIER_PATTERN = Regex("""(?i)\b(s=[0-9a-f]{32}|v=[0-9a-f]{64})\b""")

    /**
     * Sanitizes any text string before persistence:
     * - Strips multiline stack traces (replaces with safe exception summary).
     * - Redacts recovery codes, passwords, raw cryptographic hashes and private keys.
     * - Truncates length to 500 characters to prevent buffer bloat.
     */
    fun sanitizeText(input: String): String {
        if (input.isBlank()) return ""
        var text = input

        // Reject or safely reduce raw exception stack traces
        if (text.contains("Exception") && text.contains("\n\tat ")) {
            val firstLine = text.lineSequence().firstOrNull()?.trim() ?: "Exception"
            text = "SAFE_EXCEPTION: $firstLine"
        } else {
            text = text.replace("\r", " ").replace("\n", " ").trim()
        }

        text = RECOVERY_CODE_PATTERN.replace(text, "[REDACTED_RECOVERY_CODE]")
        text = GENERIC_KEY_VALUE_SECRET.replace(text) { matchResult ->
            val k = matchResult.groupValues[1]
            "$k=[REDACTED_CREDENTIAL]"
        }
        text = PHC_HASH_PATTERN.replace(text, "[PBKDF2_HASH_REDACTED]")
        text = RAW_SALT_VERIFIER_PATTERN.replace(text, "[VERIFIER_REDACTED]")

        return text.take(500)
    }

    /**
     * Sanitizes structured metadata against key allowlist and value safety policy.
     * Only approved keys are retained.
     */
    fun sanitizeMetadata(rawMetadata: String): String {
        if (rawMetadata.isBlank() || rawMetadata == "{}") return "{}"
        val pairs = mutableMapOf<String, String>()
        val clean = rawMetadata.removeSurrounding("{", "}").trim()
        val tokens = clean.split(Regex("[,;]"))
        for (token in tokens) {
            val parts = token.split(Regex("[:=]"), limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().removeSurrounding("\"").removeSurrounding("'")
                val value = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
                if (key in PERMITTED_METADATA_KEYS) {
                    pairs[key] = sanitizeText(value).take(120)
                }
            }
        }
        return if (pairs.isEmpty()) "{}" else pairs.entries.joinToString(prefix = "{", postfix = "}") {
            val isNum = it.value.toDoubleOrNull() != null
            if (isNum) "\"${it.key}\":${it.value}" else "\"${it.key}\":\"${it.value}\""
        }
    }
}

/**
 * Secure Room database schema for institutional audit logging.
 *
 * Security Requirements:
 * - Passwords, PINs, recovery codes, private cryptographic keys, and raw verifiers
 *   are STRICTLY PROHIBITED and excluded from this schema.
 * - Account attribution is strictly required with NO default fallback to prevent cross-account leakage.
 * - Multi-layer security: Allowlist validation for categories, actions, and metadata keys, plus regex redaction.
 * - Canonical length-prefixed serialization enables local tamper-evident verification.
 * - Indexed on (accountId, timestamp) and (category, action) for fast tenant-isolated auditing.
 */
@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["accountId", "timestamp"]),
        Index(value = ["category", "action"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: String, // Explicitly bound to authenticated account context (NO DEFAULT)
    val category: String, // Validated against PERMITTED_CATEGORIES
    val action: String,   // Validated against PERMITTED_ACTIONS
    val actor: String,
    val rationale: String,
    val dataAvailable: String,
    val decisionMade: String,
    val outcome: String,
    val eventMetadata: String = "{}", // Structured non-sensitive metadata (JSON format, validated against PERMITTED_METADATA_KEYS)
    val integrityHash: String = "",   // Canonical SHA-256 tamper-evident fingerprint
    val timestamp: Long = System.currentTimeMillis()
) {

    /**
     * Computes the unambiguous, length-prefixed canonical representation of the audit payload.
     */
    fun computeCanonicalPayload(): String {
        fun encode(s: String): String = "${s.length}:$s"
        return listOf(
            encode(accountId),
            encode(category),
            encode(action),
            encode(actor),
            encode(rationale),
            encode(dataAvailable),
            encode(decisionMade),
            encode(outcome),
            encode(eventMetadata),
            encode(timestamp.toString())
        ).joinToString("|")
    }

    /**
     * Calculates the SHA-256 digest of the canonical payload.
     */
    fun calculateCanonicalHash(): String {
        val payload = computeCanonicalPayload()
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies that the stored integrityHash matches the canonical payload hash.
     * Detects post-insertion database tampering, mutation, or corruption.
     */
    fun verifyIntegrity(): Boolean {
        if (integrityHash.isBlank()) return false
        val computed = calculateCanonicalHash()
        return MessageDigest.isEqual(computed.toByteArray(Charsets.UTF_8), integrityHash.toByteArray(Charsets.UTF_8))
    }

    companion object {
        /**
         * Factory function enforcing allowlist filtering, zero-leakage redaction,
         * and canonical integrity fingerprinting prior to database insertion.
         */
        fun createSecureEntry(
            accountId: String,
            category: String,
            action: String,
            actor: String,
            rationale: String,
            dataAvailable: String,
            decisionMade: String,
            outcome: String,
            eventMetadata: String = "{}",
            timestamp: Long = System.currentTimeMillis()
        ): AuditLogEntity {
            require(accountId.isNotBlank()) { "accountId must be explicitly specified and non-blank" }

            // Allowlist Category Enforcement
            val validatedCategory = if (category.uppercase() in AuditSecurityPolicy.PERMITTED_CATEGORIES) {
                category.uppercase()
            } else {
                "SECURITY"
            }

            // Allowlist Action Enforcement
            val validatedAction = if (action in AuditSecurityPolicy.PERMITTED_ACTIONS) {
                action
            } else {
                "UNAPPROVED_AUDIT_ACTION"
            }

            val cleanActor = AuditSecurityPolicy.sanitizeText(actor)
            val cleanRationale = AuditSecurityPolicy.sanitizeText(rationale)
            val cleanDataAvailable = AuditSecurityPolicy.sanitizeText(dataAvailable)
            val cleanDecisionMade = AuditSecurityPolicy.sanitizeText(decisionMade)
            val cleanOutcome = AuditSecurityPolicy.sanitizeText(outcome)
            val cleanMetadata = AuditSecurityPolicy.sanitizeMetadata(eventMetadata)

            val baseEntity = AuditLogEntity(
                accountId = accountId.trim(),
                category = validatedCategory,
                action = validatedAction,
                actor = cleanActor,
                rationale = cleanRationale,
                dataAvailable = cleanDataAvailable,
                decisionMade = cleanDecisionMade,
                outcome = cleanOutcome,
                eventMetadata = cleanMetadata,
                integrityHash = "",
                timestamp = timestamp
            )

            val canonicalHash = baseEntity.calculateCanonicalHash()
            return baseEntity.copy(integrityHash = canonicalHash)
        }
    }
}
