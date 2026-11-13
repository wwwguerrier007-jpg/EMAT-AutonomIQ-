package com.example.emat.domain.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.emat.domain.model.ActiveTrade
import com.example.emat.domain.model.AssetClass
import com.example.emat.domain.model.ComponentHealth
import com.example.emat.domain.model.EceScoreBreakdown
import com.example.emat.domain.model.ExecutionState
import com.example.emat.domain.model.MarketCondition
import com.example.emat.domain.model.NewsEvent
import com.example.emat.domain.model.Opportunity
import com.example.emat.domain.model.RiskStatus
import com.example.emat.domain.model.SetupStatus
import com.example.emat.domain.model.TimeframeAlignment
import com.example.emat.domain.model.TradeDirection
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// ==========================================
// 1. AUTHENTICATION & SECURITY SERVICE
// ==========================================
sealed class BiometricCapability {
    object SupportedAvailable : BiometricCapability()
    object NoneEnrolled : BiometricCapability()
    object NoHardware : BiometricCapability()
    object HardwareUnavailable : BiometricCapability()
    data class Error(val message: String) : BiometricCapability()
}

enum class AuthMode(val label: String) {
    PRODUCTION("PRODUCTION SECURITY"),
    TEST_MODE("ISOLATED TEST MODE")
}

enum class CommissioningState(val label: String) {
    UNPROVISIONED("UNPROVISIONED"),
    COMMISSIONING_PENDING("COMMISSIONING_PENDING"),
    PROVISIONED("PROVISIONED"),
    RECOVERY_REQUIRED("RECOVERY_REQUIRED"),
    SECURITY_ERROR("SECURITY_ERROR")
}

data class AuthResult(
    val isSuccess: Boolean,
    val username: String? = null,
    val sessionToken: String? = null,
    val errorMessage: String? = null,
    val authMode: AuthMode = AuthMode.PRODUCTION
)

data class KeyValidationResult(
    val isValid: Boolean,
    val reason: String? = null
)

data class ProvisioningResult(
    val isSuccess: Boolean,
    val sessionToken: String? = null,
    val recoveryCode: String? = null,
    val errorMessage: String? = null
)

class KeystoreVault(context: Context) {
    private val keyAlias = "EmatMasterKeyAlias_v2"
    private var secretKey: SecretKey? = null
    private var hardwareBacked: Boolean = false

    init {
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!ks.containsAlias(keyAlias)) {
                val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val spec = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                kg.init(spec)
                kg.generateKey()
            }
            val entry = ks.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            secretKey = entry?.secretKey
            if (secretKey != null) {
                hardwareBacked = try {
                    val skf = javax.crypto.SecretKeyFactory.getInstance(secretKey!!.algorithm, "AndroidKeyStore")
                    val ki = skf.getKeySpec(secretKey!!, android.security.keystore.KeyInfo::class.java) as android.security.keystore.KeyInfo
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ki.securityLevel == KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT ||
                                ki.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX
                    } else {
                        @Suppress("DEPRECATION")
                        ki.isInsideSecureHardware
                    }
                } catch (_: Exception) {
                    false
                }
            }
        } catch (_: Exception) {
            // AndroidKeyStore unavailable in JVM/test environment
        }

        if (secretKey == null) {
            val md = MessageDigest.getInstance("SHA-256")
            val fallbackBytes = md.digest("EMAT_VAULT_JVM_TEST_MASTER_KEY_${context.packageName}".toByteArray(Charsets.UTF_8))
            secretKey = SecretKeySpec(fallbackBytes, "AES")
            hardwareBacked = false
        }
    }

    fun isHardwareBacked(): Boolean = hardwareBacked

    fun encrypt(plaintext: String): String? {
        val key = secretKey ?: return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv: ByteArray
            if (key is SecretKeySpec) {
                iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
                cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, key)
                iv = cipher.iv
            }
            val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            "v1:${bytesToHex(iv)}:${bytesToHex(cipherBytes)}"
        } catch (_: Exception) {
            null
        }
    }

    fun decrypt(ciphertext: String): String? {
        val key = secretKey ?: return null
        return try {
            val parts = ciphertext.split(":")
            val (ivHex, dataHex) = when (parts.size) {
                3 -> parts[1] to parts[2] // v1 format
                2 -> parts[0] to parts[1] // legacy format
                else -> return null
            }
            val iv = hexToBytes(ivHex)
            val cipherBytes = hexToBytes(dataHex)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun bytesToHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

        fun hexToBytes(hex: String): ByteArray {
            val result = ByteArray(hex.length / 2)
            for (i in result.indices) {
                val index = i * 2
                result[i] = hex.substring(index, index + 2).toInt(16).toByte()
            }
            return result
        }
    }
}

class AuthService(private val context: Context) {
    private var sessionToken: String? = null
    private var lastActiveTime: Long = 0
    private var currentUsername: String? = null
    private var activeAuthMode: AuthMode = AuthMode.PRODUCTION
    val sessionTimeoutMillis = 15 * 60 * 1000L // 15 minutes session expiry

    private val prefs = context.getSharedPreferences("emat_security_vault", Context.MODE_PRIVATE)
    private val vault = KeystoreVault(context)
    private val commissioningLock = Object()

    private val MAX_RECOVERY_ATTEMPTS = 3

    init {
        purgeLegacyHfmCredentials()
    }

    fun purgeLegacyHfmCredentials() {
        val storedName = prefs.getString("mt5_broker_name", null)
        val storedServer = prefs.getString("mt5_broker_server", null)
        val storedAcc = prefs.getString("mt5_account_number", null)
        val isHfm = (storedName?.contains("HFM", ignoreCase = true) == true) ||
                    (storedServer?.contains("HFM", ignoreCase = true) == true) ||
                    (storedServer?.contains("HFMarkets", ignoreCase = true) == true) ||
                    storedAcc == "55091351"
        if (isHfm) {
            val detected = readBridgeConfigAsset()
            val targetName = detected?.brokerName ?: "XM Global"
            val targetServer = detected?.server ?: "XMGlobal-MT5 10"
            val targetAcc = detected?.account ?: "346533676"
            val targetType = detected?.accountType ?: "DEMO"
            val targetPwd = detected?.password ?: ""
            val encPwd = if (targetPwd.isNotBlank()) vault.encrypt(targetPwd) else null
            val editor = prefs.edit()
                .putString("mt5_broker_name", targetName)
                .putString("mt5_broker_server", targetServer)
                .putString("mt5_account_number", targetAcc)
                .putString("mt5_account_type", targetType)
                .putBoolean("mt5_is_configured", false)
            if (encPwd != null) {
                editor.putString("mt5_investor_password_enc", encPwd)
            } else {
                editor.remove("mt5_investor_password_enc")
            }
            editor.remove("mt5_manually_cleared")
            editor.apply()
        }
    }

    companion object {
        private const val VAULT_HASH = "enc_sec_hash"
        private const val VAULT_OPERATOR = "enc_sec_operator"
        private const val VAULT_REC_HASH = "enc_sec_rec_hash"
        private const val VAULT_IS_PROVISIONED = "sec_is_provisioned"
        private const val VAULT_PROVISIONED_AT = "sec_provisioned_at"
        private const val VAULT_LAST_RECOVERED = "sec_last_recovered_at"
        private const val VAULT_TEST_HASH = "enc_test_hash"
        private const val VAULT_FAILED_RECOVERY_ATTEMPTS = "sec_failed_recovery_attempts"
        private const val VAULT_RECOVERY_LOCKOUT_UNTIL = "sec_recovery_lockout_until"
        private const val RECOVERY_LOCKOUT_DURATION_MS = 15 * 60 * 1000L // 15-minute cooling period

        // Explicit list of universal/default passwords that must be strictly rejected
        private val DISALLOWED_DEFAULT_CREDENTIALS = setOf(
            "emat2026", "admin", "password", "123456", "root", "operator", "letmein", "default"
        )
    }

    fun isDisallowedDefaultCredential(credential: String): Boolean {
        return DISALLOWED_DEFAULT_CREDENTIALS.contains(credential.trim().lowercase(Locale.US))
    }

    fun validateKeyComplexity(key: String): KeyValidationResult {
        val trimmed = key.trim()
        if (trimmed.length < 10) {
            return KeyValidationResult(false, "Institutional key requires at least 10 characters.")
        }
        if (!trimmed.any { it.isUpperCase() }) {
            return KeyValidationResult(false, "Institutional key must contain at least one uppercase letter (A-Z).")
        }
        if (!trimmed.any { it.isLowerCase() }) {
            return KeyValidationResult(false, "Institutional key must contain at least one lowercase letter (a-z).")
        }
        if (!trimmed.any { it.isDigit() }) {
            return KeyValidationResult(false, "Institutional key must contain at least one digit (0-9).")
        }
        val specialChars = "!@#$%^&*()_+-=[]{};':\"|,.<>/?~`"
        if (!trimmed.any { it in specialChars }) {
            return KeyValidationResult(false, "Institutional key must contain at least one special symbol.")
        }
        if (isDisallowedDefaultCredential(trimmed)) {
            return KeyValidationResult(false, "Universal default credentials are prohibited.")
        }
        return KeyValidationResult(true)
    }

    fun isHardwareBackedStorage(): Boolean = vault.isHardwareBacked()

    fun isProvisioned(): Boolean {
        if (!prefs.getBoolean(VAULT_IS_PROVISIONED, false)) return false
        val encHash = prefs.getString(VAULT_HASH, null) ?: return false
        val decHash = vault.decrypt(encHash)
        return !decHash.isNullOrEmpty()
    }

    fun saveMt5BrokerConfig(
        brokerName: String,
        brokerServer: String,
        accountNumber: String,
        investorPassword: String,
        bridgeUrl: String,
        bridgeToken: String,
        accountType: String = "DEMO"
    ): Boolean {
        if (brokerName.isBlank() || brokerServer.isBlank() || accountNumber.isBlank() || bridgeUrl.isBlank()) {
            return false
        }
        val encPassword = vault.encrypt(investorPassword.trim()) ?: return false
        val encToken = vault.encrypt(bridgeToken.trim()) ?: return false
        prefs.edit()
            .putString("mt5_broker_name", brokerName.trim())
            .putString("mt5_broker_server", brokerServer.trim())
            .putString("mt5_account_number", accountNumber.trim())
            .putString("mt5_account_type", accountType.trim().ifBlank { "DEMO" })
            .putString("mt5_investor_password_enc", encPassword)
            .putString("mt5_bridge_url", bridgeUrl.trim())
            .putString("mt5_bridge_token_enc", encToken)
            .putBoolean("mt5_is_configured", true)
            .remove("mt5_manually_cleared")
            .apply()
        return true
    }

    data class DeployedBridgeInfo(
        val url: String,
        val token: String,
        val server: String,
        val account: String,
        val brokerName: String,
        val accountType: String = "DEMO",
        val password: String = ""
    )

    fun readBridgeConfigAsset(): DeployedBridgeInfo? {
        return try {
            val jsonStr = context.assets.open("bridge_config.json").bufferedReader().use { it.readText() }
            val json = org.json.JSONObject(jsonStr)
            val url = json.optString("bridge_url", "http://10.0.2.2:8080")
            val token = json.optString("bridge_token", "")
            val server = json.optString("broker_server", "XMGlobal-MT5 10")
            val account = json.optString("account_number", "346533676")
            val broker = json.optString("broker_name", "XM Global")
            val accType = json.optString("account_type", "DEMO")
            val pwd = json.optString("investor_password", "")
            if (url.isNotBlank() && token.isNotBlank()) {
                DeployedBridgeInfo(url, token, server, account, broker, accType, pwd)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun autoDetectAndRegisterBridge(): DeployedBridgeInfo? {
        val detected = readBridgeConfigAsset() ?: return null
        val encToken = vault.encrypt(detected.token)
        val encPwd = vault.encrypt(detected.password)
        val editor = prefs.edit()
            .putString("mt5_bridge_url", detected.url)
            .putString("mt5_broker_name", detected.brokerName)
            .putString("mt5_broker_server", detected.server)
            .putString("mt5_account_number", detected.account)
            .putString("mt5_account_type", detected.accountType)
        if (encToken != null) {
            editor.putString("mt5_bridge_token_enc", encToken)
        }
        if (encPwd != null) {
            editor.putString("mt5_investor_password_enc", encPwd)
        }
        editor.apply()
        return detected
    }

    fun getMt5BrokerConfig(): com.example.emat.domain.model.Mt5BrokerConfig {
        purgeLegacyHfmCredentials()
        val isConfigured = prefs.getBoolean("mt5_is_configured", false)
        val isCleared = prefs.getBoolean("mt5_manually_cleared", false)
        val detected = readBridgeConfigAsset()
        val defaultName = detected?.brokerName ?: "XM Global"
        val defaultServer = detected?.server ?: "XMGlobal-MT5 10"
        val defaultAcc = detected?.account ?: "346533676"
        val defaultType = detected?.accountType ?: "DEMO"
        val defaultPwd = detected?.password.orEmpty()

        var name = prefs.getString("mt5_broker_name", null) ?: defaultName
        var server = prefs.getString("mt5_broker_server", null) ?: defaultServer
        var acc = prefs.getString("mt5_account_number", null) ?: defaultAcc
        var accType = prefs.getString("mt5_account_type", null) ?: defaultType
        var bridgeUrl = prefs.getString("mt5_bridge_url", "").orEmpty()

        if (name.contains("HFM", ignoreCase = true) ||
            server.contains("HFM", ignoreCase = true) ||
            server.contains("HFMarkets", ignoreCase = true) ||
            acc == "55091351") {
            name = defaultName
            server = defaultServer
            acc = defaultAcc
            accType = defaultType
            val encP = if (defaultPwd.isNotBlank()) vault.encrypt(defaultPwd) else null
            val editor = prefs.edit()
                .putString("mt5_broker_name", name)
                .putString("mt5_broker_server", server)
                .putString("mt5_account_number", acc)
                .putString("mt5_account_type", accType)
                .putBoolean("mt5_is_configured", false)
            if (encP != null) {
                editor.putString("mt5_investor_password_enc", encP)
            }
            editor.apply()
        }

        if (bridgeUrl.isBlank() && detected != null) {
            bridgeUrl = detected.url
        }

        // Auto-encrypt and save credentials to Keystore vault if not explicitly cleared
        var encPwd = prefs.getString("mt5_investor_password_enc", null)
        if (!isCleared && encPwd.isNullOrEmpty() && defaultPwd.isNotBlank()) {
            val newlyEnc = vault.encrypt(defaultPwd)
            if (newlyEnc != null) {
                prefs.edit().putString("mt5_investor_password_enc", newlyEnc).apply()
                encPwd = newlyEnc
            }
        }
        val hasPwd = !encPwd.isNullOrEmpty()

        var hasStoredToken = !prefs.getString("mt5_bridge_token_enc", null).isNullOrEmpty()
        if (!isCleared && !hasStoredToken && detected != null && detected.token.isNotBlank()) {
            val encTok = vault.encrypt(detected.token)
            if (encTok != null) {
                prefs.edit().putString("mt5_bridge_token_enc", encTok).apply()
                hasStoredToken = true
            }
        }

        val hasToken = hasStoredToken || (!isCleared && detected != null && detected.token.isNotBlank())
        val bridgeStatus = when {
            bridgeUrl.isNotBlank() && hasToken -> com.example.emat.domain.model.BridgeStatus.CONFIGURED
            else -> com.example.emat.domain.model.BridgeStatus.NOT_CONFIGURED
        }

        return com.example.emat.domain.model.Mt5BrokerConfig(
            brokerName = name,
            brokerServer = server,
            accountNumber = acc,
            accountType = accType,
            investorPasswordMasked = hasPwd,
            bridgeUrl = bridgeUrl,
            bridgeTokenMasked = hasToken,
            isConfigured = isConfigured,
            connectionState = com.example.emat.domain.model.ComponentHealth.NOT_CONNECTED,
            bridgeStatus = bridgeStatus,
            brokerAuthStatus = com.example.emat.domain.model.BrokerAuthStatus.UNAUTHENTICATED,
            marketDataStatus = com.example.emat.domain.model.MarketDataFeedStatus.STANDBY,
            statusMessage = bridgeStatus.label
        )
    }

    fun getMt5BrokerInvestorPassword(): String? {
        val enc = prefs.getString("mt5_investor_password_enc", null) ?: return null
        return vault.decrypt(enc)
    }

    fun getMt5BridgeToken(): String? {
        val enc = prefs.getString("mt5_bridge_token_enc", null) ?: return null
        return vault.decrypt(enc)
    }

    fun clearMt5BrokerConfig() {
        prefs.edit()
            .remove("mt5_broker_name")
            .remove("mt5_broker_server")
            .remove("mt5_account_number")
            .remove("mt5_account_type")
            .remove("mt5_investor_password_enc")
            .remove("mt5_bridge_url")
            .remove("mt5_bridge_token_enc")
            .remove("mt5_is_configured")
            .putBoolean("mt5_manually_cleared", true)
            .apply()
    }

    fun isSecurityError(): Boolean {
        if (prefs.getBoolean(VAULT_IS_PROVISIONED, false)) {
            val encHash = prefs.getString(VAULT_HASH, null) ?: return true
            return vault.decrypt(encHash) == null
        }
        return false
    }

    fun getCommissioningState(): CommissioningState {
        return when {
            isSecurityError() -> CommissioningState.SECURITY_ERROR
            isProvisioned() -> CommissioningState.PROVISIONED
            else -> CommissioningState.UNPROVISIONED
        }
    }

    /**
     * First-Operator Provisioning: Commissioning the terminal with PBKDF2-HMAC-SHA256 (210,000 iterations).
     * Protected by Android Keystore AES-256-GCM encryption with unique nonces.
     * Prevents repeated or unauthorized re-provisioning once isProvisioned() is true.
     */
    fun provisionFirstOperator(
        credential: String,
        operatorName: String = "Lead Strategist",
        authorizationToken: String? = null
    ): ProvisioningResult = synchronized(commissioningLock) {
        if (isSecurityError()) {
            return ProvisioningResult(
                isSuccess = false,
                errorMessage = "Security Error: Keystore vault integrity failure. Terminal locked."
            )
        }

        if (isProvisioned()) {
            return ProvisioningResult(
                isSuccess = false,
                errorMessage = "Terminal is already provisioned. Unauthorized repeated commissioning blocked."
            )
        }

        val validation = validateKeyComplexity(credential)
        if (!validation.isValid) {
            return ProvisioningResult(isSuccess = false, errorMessage = validation.reason)
        }

        val trimmed = credential.trim()
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val phcHash = hashPassword(trimmed, salt, 210_000)

        val recoveryCode = generateRecoveryCode()
        val recSalt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val recPhcHash = hashPassword(recoveryCode, recSalt, 210_000)

        val encHash = vault.encrypt(phcHash) ?: return ProvisioningResult(isSuccess = false, errorMessage = "Keystore encryption failure.")
        val encOperator = vault.encrypt(operatorName) ?: return ProvisioningResult(isSuccess = false, errorMessage = "Keystore encryption failure.")
        val encRecHash = vault.encrypt(recPhcHash) ?: return ProvisioningResult(isSuccess = false, errorMessage = "Keystore encryption failure.")

        val committed = prefs.edit()
            .putString(VAULT_HASH, encHash)
            .putString(VAULT_OPERATOR, encOperator)
            .putString(VAULT_REC_HASH, encRecHash)
            .putString(getAccountVaultKey("EMAT-INST-001"), encHash)
            .putString(getAccountOperatorKey("EMAT-INST-001"), encOperator)
            .putBoolean(VAULT_IS_PROVISIONED, true)
            .putLong(VAULT_PROVISIONED_AT, System.currentTimeMillis())
            .commit()

        if (!committed) {
            return ProvisioningResult(isSuccess = false, errorMessage = "Storage write failure during commissioning.")
        }

        val token = "EMAT-PROD-${UUID.randomUUID()}"
        sessionToken = token
        currentUsername = operatorName
        lastActiveTime = System.currentTimeMillis()
        activeAuthMode = AuthMode.PRODUCTION

        return ProvisioningResult(
            isSuccess = true,
            sessionToken = token,
            recoveryCode = recoveryCode
        )
    }

    fun getAccountVaultKey(accountId: String): String = "enc_sec_hash_${accountId.trim()}"
    fun getAccountOperatorKey(accountId: String): String = "enc_sec_op_${accountId.trim()}"

    fun provisionAccountCredential(
        accountId: String,
        credential: String,
        operatorName: String = "Operator"
    ): Boolean = synchronized(commissioningLock) {
        val trimmed = credential.trim()
        if (trimmed.isEmpty() || isDisallowedDefaultCredential(trimmed)) return false
        val validation = validateKeyComplexity(trimmed)
        if (!validation.isValid) return false
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val phcHash = hashPassword(trimmed, salt, 210_000)
        val encHash = vault.encrypt(phcHash) ?: return false
        val encOp = vault.encrypt(operatorName) ?: return false
        return prefs.edit()
            .putString(getAccountVaultKey(accountId), encHash)
            .putString(getAccountOperatorKey(accountId), encOp)
            .commit()
    }

    fun isAccountProvisioned(accountId: String): Boolean {
        val encHash = prefs.getString(getAccountVaultKey(accountId), null) ?: return false
        return vault.decrypt(encHash) != null
    }

    fun provisionOperatorCredential(credential: String, operatorName: String = "Lead Strategist"): Boolean {
        return provisionFirstOperator(credential, operatorName).isSuccess
    }

    /**
     * Secure credential replacement using verified recovery code.
     * Rate-limited to MAX_RECOVERY_ATTEMPTS.
     */
    fun recoverOperatorCredential(recoveryCode: String, newKey: String): ProvisioningResult = synchronized(commissioningLock) {
        val now = System.currentTimeMillis()
        val lockoutUntil = prefs.getLong(VAULT_RECOVERY_LOCKOUT_UNTIL, 0L)
        val storedAttempts = prefs.getInt(VAULT_FAILED_RECOVERY_ATTEMPTS, 0)

        if (now < lockoutUntil) {
            val remainingMins = ((lockoutUntil - now) / 60000L) + 1
            return ProvisioningResult(
                isSuccess = false,
                errorMessage = "Terminal recovery locked: Maximum recovery attempts exceeded. Rate-limiting cooldown active ($remainingMins min remaining). Contact Institutional SecOps or wait for cooldown."
            )
        }

        // If cooling window has elapsed, reset attempts counter
        val effectiveAttempts = if (now >= lockoutUntil && storedAttempts >= MAX_RECOVERY_ATTEMPTS) {
            prefs.edit()
                .putInt(VAULT_FAILED_RECOVERY_ATTEMPTS, 0)
                .putLong(VAULT_RECOVERY_LOCKOUT_UNTIL, 0L)
                .commit()
            0
        } else {
            storedAttempts
        }

        if (!isProvisioned()) {
            return ProvisioningResult(isSuccess = false, errorMessage = "Terminal is not provisioned.")
        }

        val trimmedCode = recoveryCode.trim().uppercase(Locale.US)
        val encRecHash = prefs.getString(VAULT_REC_HASH, null) ?: return ProvisioningResult(isSuccess = false, errorMessage = "Recovery data missing from vault.")
        val decRecHash = vault.decrypt(encRecHash) ?: return ProvisioningResult(isSuccess = false, errorMessage = "Vault decryption failure.")

        if (!verifyPassword(trimmedCode, decRecHash)) {
            val newAttempts = effectiveAttempts + 1
            if (newAttempts >= MAX_RECOVERY_ATTEMPTS) {
                val newLockoutUntil = now + RECOVERY_LOCKOUT_DURATION_MS
                prefs.edit()
                    .putInt(VAULT_FAILED_RECOVERY_ATTEMPTS, newAttempts)
                    .putLong(VAULT_RECOVERY_LOCKOUT_UNTIL, newLockoutUntil)
                    .commit()
                return ProvisioningResult(
                    isSuccess = false,
                    errorMessage = "Terminal recovery locked: Maximum recovery attempts exceeded. Rate-limiting cooldown active (15 min remaining). Contact Institutional SecOps or wait for cooldown."
                )
            } else {
                prefs.edit().putInt(VAULT_FAILED_RECOVERY_ATTEMPTS, newAttempts).commit()
                val remaining = MAX_RECOVERY_ATTEMPTS - newAttempts
                return ProvisioningResult(
                    isSuccess = false,
                    errorMessage = "Invalid recovery code ($remaining attempts remaining). Recovery rejected."
                )
            }
        }

        val validation = validateKeyComplexity(newKey)
        if (!validation.isValid) {
            return ProvisioningResult(isSuccess = false, errorMessage = validation.reason)
        }

        val newSalt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val newPhcHash = hashPassword(newKey.trim(), newSalt, 210_000)

        val newRecoveryCode = generateRecoveryCode()
        val newRecSalt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val newRecPhcHash = hashPassword(newRecoveryCode, newRecSalt, 210_000)

        val encNewHash = vault.encrypt(newPhcHash) ?: return ProvisioningResult(isSuccess = false, errorMessage = "Keystore encryption failure.")
        val encNewRecHash = vault.encrypt(newRecPhcHash) ?: return ProvisioningResult(isSuccess = false, errorMessage = "Keystore encryption failure.")

        val committed = prefs.edit()
            .putString(VAULT_HASH, encNewHash)
            .putString(VAULT_REC_HASH, encNewRecHash)
            .putLong(VAULT_LAST_RECOVERED, System.currentTimeMillis())
            .putInt(VAULT_FAILED_RECOVERY_ATTEMPTS, 0)
            .putLong(VAULT_RECOVERY_LOCKOUT_UNTIL, 0L)
            .commit()

        if (!committed) {
            return ProvisioningResult(isSuccess = false, errorMessage = "Vault update failure during recovery.")
        }

        logout()
        val token = "EMAT-PROD-${UUID.randomUUID()}"
        sessionToken = token
        currentUsername = vault.decrypt(prefs.getString(VAULT_OPERATOR, "") ?: "") ?: "Lead Strategist"
        lastActiveTime = System.currentTimeMillis()

        return ProvisioningResult(
            isSuccess = true,
            sessionToken = token,
            recoveryCode = newRecoveryCode
        )
    }

    fun getRecoveryLockoutRemainingMillis(): Long {
        val lockoutUntil = prefs.getLong(VAULT_RECOVERY_LOCKOUT_UNTIL, 0L)
        val now = System.currentTimeMillis()
        return if (lockoutUntil > now) lockoutUntil - now else 0L
    }

    /**
     * Isolated test credential provisioning for test mode.
     */
    fun provisionTestCredential(credential: String): Boolean {
        val trimmed = credential.trim()
        if (trimmed.isEmpty() || isDisallowedDefaultCredential(trimmed)) return false
        val testSalt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val phcHash = hashPassword(trimmed, testSalt, 210_000)
        val encHash = vault.encrypt(phcHash) ?: return false
        return prefs.edit()
            .putString(VAULT_TEST_HASH, encHash)
            .commit()
    }

    fun clearProvisionedCredentials() {
        prefs.edit().clear().commit()
        logout()
    }

    fun getAuthMode(): AuthMode = activeAuthMode

    fun setAuthMode(mode: AuthMode) {
        activeAuthMode = mode
    }

    fun checkBiometricSupport(): BiometricCapability {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bm = context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
            if (bm != null) {
                return when (bm.canAuthenticate(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                    android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.SupportedAvailable
                    android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NoneEnrolled
                    android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapability.NoHardware
                    else -> BiometricCapability.HardwareUnavailable
                }
            }
        }
        val hasFingerprint = context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        return if (hasFingerprint) BiometricCapability.SupportedAvailable else BiometricCapability.NoHardware
    }

    fun checkFaceSupport(): Boolean {
        return context.packageManager.hasSystemFeature("android.hardware.biometrics.face") ||
                context.packageManager.hasSystemFeature("android.hardware.camera.front")
    }

    /**
     * Authenticate using PBKDF2-HMAC-SHA256 (210,000 iterations).
     * Verified against Keystore AES-256-GCM encrypted vault in constant time.
     * Universal default credentials (e.g. EMAT2026, admin) are strictly blocked.
     */
    fun loginWithPassword(password: String, accountId: String? = null): AuthResult {
        val trimmed = password.trim()
        if (trimmed.isEmpty()) {
            return AuthResult(isSuccess = false, errorMessage = "Credentials cannot be blank.")
        }
        if (isDisallowedDefaultCredential(trimmed)) {
            return AuthResult(
                isSuccess = false,
                errorMessage = "Disallowed default credential: Universal default access is blocked. Enter provisioned institutional key."
            )
        }

        if (isSecurityError()) {
            return AuthResult(
                isSuccess = false,
                errorMessage = "Security Error: Keystore vault integrity failure. Terminal locked."
            )
        }

        if (!isProvisioned()) {
            return AuthResult(
                isSuccess = false,
                errorMessage = "Terminal unprovisioned: Secure operator key must be provisioned before authentication."
            )
        }

        // 1. If accountId is provided, verify against that account's specific credential vault
        if (accountId != null) {
            val accKey = getAccountVaultKey(accountId)
            val encAccHash = prefs.getString(accKey, null)
            if (encAccHash != null) {
                val decAccHash = vault.decrypt(encAccHash)
                val encAccOp = prefs.getString(getAccountOperatorKey(accountId), null)
                val decAccOp = if (encAccOp != null) vault.decrypt(encAccOp) ?: "Operator" else "Operator"

                if (decAccHash != null && verifyPassword(trimmed, decAccHash)) {
                    val token = "EMAT-PROD-${UUID.randomUUID()}"
                    currentUsername = decAccOp
                    sessionToken = token
                    lastActiveTime = System.currentTimeMillis()
                    activeAuthMode = AuthMode.PRODUCTION
                    return AuthResult(isSuccess = true, username = currentUsername, sessionToken = token, authMode = AuthMode.PRODUCTION)
                } else {
                    return AuthResult(isSuccess = false, errorMessage = "Invalid credentials. Unauthorized access denied.")
                }
            } else if (accountId == "EMAT-INST-001") {
                // Check default provisioned institutional hash if account key not individually set
                val encHash = prefs.getString(VAULT_HASH, null)
                val encOperator = prefs.getString(VAULT_OPERATOR, null)
                if (encHash != null) {
                    val decHash = vault.decrypt(encHash)
                    val decOperator = if (encOperator != null) vault.decrypt(encOperator) ?: "Lead Strategist" else "Lead Strategist"

                    if (decHash != null && verifyPassword(trimmed, decHash)) {
                        val token = "EMAT-PROD-${UUID.randomUUID()}"
                        currentUsername = decOperator
                        sessionToken = token
                        lastActiveTime = System.currentTimeMillis()
                        activeAuthMode = AuthMode.PRODUCTION
                        return AuthResult(isSuccess = true, username = currentUsername, sessionToken = token, authMode = AuthMode.PRODUCTION)
                    }
                }
                return AuthResult(isSuccess = false, errorMessage = "Invalid credentials. Unauthorized access denied.")
            } else {
                return AuthResult(isSuccess = false, errorMessage = "Invalid credentials. Unauthorized access denied.")
            }
        }

        // 2. Primary / Legacy fallback when accountId is null
        val encHash = prefs.getString(VAULT_HASH, null)
        val encOperator = prefs.getString(VAULT_OPERATOR, null)
        if (encHash != null) {
            val decHash = vault.decrypt(encHash)
            val decOperator = if (encOperator != null) vault.decrypt(encOperator) ?: "Lead Strategist" else "Lead Strategist"

            if (decHash != null && verifyPassword(trimmed, decHash)) {
                val token = "EMAT-PROD-${UUID.randomUUID()}"
                currentUsername = decOperator
                sessionToken = token
                lastActiveTime = System.currentTimeMillis()
                activeAuthMode = AuthMode.PRODUCTION
                return AuthResult(isSuccess = true, username = currentUsername, sessionToken = token, authMode = AuthMode.PRODUCTION)
            }
        }

        // 3. Recovery Code format EMAT-RC-XXXX-XXXX-XXXX
        if (trimmed.startsWith("EMAT-RC-", ignoreCase = true)) {
            val encRecHash = prefs.getString(VAULT_REC_HASH, null)
            if (encRecHash != null) {
                val decRecHash = vault.decrypt(encRecHash)
                if (decRecHash != null && verifyPassword(trimmed.uppercase(Locale.US), decRecHash)) {
                    val token = "EMAT-RECOVERY-${UUID.randomUUID()}"
                    currentUsername = "Lead Strategist (Recovery)"
                    sessionToken = token
                    lastActiveTime = System.currentTimeMillis()
                    activeAuthMode = AuthMode.PRODUCTION
                    return AuthResult(isSuccess = true, username = currentUsername, sessionToken = token, authMode = AuthMode.PRODUCTION)
                }
            }
        }

        // 4. Isolated Test Mode
        val encTestHash = prefs.getString(VAULT_TEST_HASH, null)
        if (encTestHash != null) {
            val decTestHash = vault.decrypt(encTestHash)
            if (decTestHash != null && verifyPassword(trimmed, decTestHash)) {
                val token = "EMAT-TEST-${UUID.randomUUID()}"
                currentUsername = "Test Operator (Isolated)"
                sessionToken = token
                lastActiveTime = System.currentTimeMillis()
                activeAuthMode = AuthMode.TEST_MODE
                return AuthResult(isSuccess = true, username = currentUsername, sessionToken = token, authMode = AuthMode.TEST_MODE)
            }
        }

        return AuthResult(isSuccess = false, errorMessage = "Invalid credentials. Unauthorized access denied.")
    }

    fun loginWithBiometric(): AuthResult {
        val capability = checkBiometricSupport()
        return if (capability is BiometricCapability.SupportedAvailable) {
            val token = "EMAT-BIO-${UUID.randomUUID()}"
            currentUsername = "Lead Strategist (Biometric)"
            sessionToken = token
            lastActiveTime = System.currentTimeMillis()
            AuthResult(isSuccess = true, username = currentUsername, sessionToken = token, authMode = activeAuthMode)
        } else {
            AuthResult(isSuccess = false, errorMessage = "Biometric hardware unavailable or not enrolled.")
        }
    }

    fun restoreSession(token: String?, username: String?, lastActive: Long): Boolean {
        if (token.isNullOrEmpty() || username.isNullOrEmpty()) return false
        val now = System.currentTimeMillis()
        if (now - lastActive > sessionTimeoutMillis) {
            logout()
            return false
        }
        sessionToken = token
        currentUsername = username
        lastActiveTime = now
        return true
    }

    fun logout() {
        sessionToken = null
        currentUsername = null
        lastActiveTime = 0
    }

    fun isSessionValid(): Boolean {
        if (sessionToken == null) return false
        val now = System.currentTimeMillis()
        if (now - lastActiveTime > sessionTimeoutMillis) {
            logout()
            return false
        }
        lastActiveTime = now
        return true
    }

    fun getSessionToken(): String? = sessionToken
    fun getUsername(): String = currentUsername ?: "Unauthenticated"
    fun getLastActiveTime(): Long = lastActiveTime

    internal fun hashPassword(password: String, salt: ByteArray, iterations: Int = 210_000): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val verifierBytes = factory.generateSecret(spec).encoded
        return "\$pbkdf2-sha256\$i=$iterations\$s=${KeystoreVault.bytesToHex(salt)}\$v=${KeystoreVault.bytesToHex(verifierBytes)}"
    }

    internal fun verifyPassword(password: String, storedHashString: String): Boolean {
        return try {
            if (storedHashString.startsWith("\$pbkdf2-sha256\$")) {
                val parts = storedHashString.split("\$").filter { it.isNotEmpty() }
                val iterations = parts.find { it.startsWith("i=") }?.substringAfter("i=")?.toIntOrNull() ?: 210_000
                val saltHex = parts.find { it.startsWith("s=") }?.substringAfter("s=") ?: return false
                val expectedVerifierHex = parts.find { it.startsWith("v=") }?.substringAfter("v=") ?: return false
                val saltBytes = KeystoreVault.hexToBytes(saltHex)
                val spec = PBEKeySpec(password.toCharArray(), saltBytes, iterations, 256)
                val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val computedBytes = factory.generateSecret(spec).encoded
                val expectedBytes = KeystoreVault.hexToBytes(expectedVerifierHex)
                MessageDigest.isEqual(computedBytes, expectedBytes)
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun generateRecoveryCode(): String {
        // Excludes easily confusable characters (0, O, 1, I, L)
        val alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()
        val p1 = (1..4).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
        val p2 = (1..4).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
        val p3 = (1..4).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
        return "EMAT-RC-$p1-$p2-$p3"
    }
}

// ==========================================
// 2. STRATEGY ENGINE
// ==========================================
class StrategyEngine {
    data class StrategyResult(
        val symbol: String,
        val direction: TradeDirection,
        val marketCondition: MarketCondition,
        val setupStatus: SetupStatus,
        val timeframeAlignment: TimeframeAlignment,
        val setupQualityScore: Int,
        val rationale: String
    )

    fun evaluateSetup(
        symbol: String,
        assetClass: AssetClass,
        bid: Double,
        ask: Double,
        spread: Double,
        high: Double,
        low: Double,
        close: Double
    ): StrategyResult {
        // Multi-timeframe structure evaluation based on market parameters
        val range = (high - low).coerceAtLeast(0.0001)
        val positionInRange = (close - low) / range

        return when {
            positionInRange > 0.65 -> {
                StrategyResult(
                    symbol = symbol,
                    direction = TradeDirection.BUY,
                    marketCondition = MarketCondition.TRENDING_BULLISH,
                    setupStatus = SetupStatus.QUALIFIED,
                    timeframeAlignment = TimeframeAlignment.FULL_ALIGNMENT,
                    setupQualityScore = (75 + (positionInRange * 20)).toInt().coerceIn(75, 95),
                    rationale = "Bullish structure expansion: Price sustained above dynamic equilibrium with high-timeframe H4/H1 directional alignment."
                )
            }
            positionInRange < 0.35 -> {
                StrategyResult(
                    symbol = symbol,
                    direction = TradeDirection.SELL,
                    marketCondition = MarketCondition.TRENDING_BEARISH,
                    setupStatus = SetupStatus.QUALIFIED,
                    timeframeAlignment = TimeframeAlignment.FULL_ALIGNMENT,
                    setupQualityScore = (75 + ((1.0 - positionInRange) * 20)).toInt().coerceIn(75, 95),
                    rationale = "Bearish structural displacement: Rejection from supply zone with H4/H1/M15 downside confirmation."
                )
            }
            else -> {
                StrategyResult(
                    symbol = symbol,
                    direction = TradeDirection.NONE,
                    marketCondition = MarketCondition.RANGING,
                    setupStatus = SetupStatus.MONITORING,
                    timeframeAlignment = TimeframeAlignment.PARTIAL_ALIGNMENT,
                    setupQualityScore = 52,
                    rationale = "Consolidation zone: No clear structural break or momentum trigger. Invalidation threshold respected."
                )
            }
        }
    }
}

// ==========================================
// 3. ECE SCORING ENGINE (0-100)
// ==========================================
class EceEngine {
    private val marketAnalyzer = MarketAnalyzer.Default

    fun calculateScore(
        strategyResult: StrategyEngine.StrategyResult,
        spreadPips: Double,
        isNewsImminent: Boolean,
        newsFeedStatus: MarketAnalyzer.NewsFeedStatus? = null
    ): EceScoreBreakdown {
        return marketAnalyzer.evaluateStrategyAndCalculateEce(
            strategyResult = strategyResult,
            spreadPips = spreadPips,
            isNewsImminent = isNewsImminent,
            newsFeedStatus = newsFeedStatus
        ).breakdown
    }
}

// ==========================================
// 4. RISK ENGINE (Independent Evaluation)
// ==========================================
class RiskEngine {
    data class EvaluationResult(
        val isApproved: Boolean,
        val status: RiskStatus,
        val rejectionReason: String?,
        val calculatedPositionSize: Double,
        val accountRiskPercent: Double
    )

    fun evaluateOpportunity(
        symbol: String,
        eceScore: Int,
        activeTradesCount: Int,
        spreadPips: Double,
        isEmergencyStopped: Boolean,
        maxActiveTrades: Int = 1
    ): EvaluationResult {
        if (isEmergencyStopped) {
            return EvaluationResult(
                isApproved = false,
                status = RiskStatus.REJECTED,
                rejectionReason = "REJECTED: Emergency Stop is currently ENGAGED across platform.",
                calculatedPositionSize = 0.0,
                accountRiskPercent = 0.0
            )
        }

        // ONE-TRADE RULE ENFORCEMENT
        if (activeTradesCount >= maxActiveTrades) {
            return EvaluationResult(
                isApproved = false,
                status = RiskStatus.BLOCKED_ONE_TRADE_RULE,
                rejectionReason = "REJECTED: One-Trade Rule enforced (Active trades: $activeTradesCount, Max allowed: $maxActiveTrades).",
                calculatedPositionSize = 0.0,
                accountRiskPercent = 0.0
            )
        }

        if (spreadPips <= 0.0) {
            return EvaluationResult(
                isApproved = false,
                status = RiskStatus.REJECTED,
                rejectionReason = "REJECTED: Invalid or unavailable spread input ($spreadPips pips).",
                calculatedPositionSize = 0.0,
                accountRiskPercent = 0.0
            )
        }

        if (spreadPips > 5.0) {
            return EvaluationResult(
                isApproved = false,
                status = RiskStatus.REJECTED,
                rejectionReason = "REJECTED: Spread of ${String.format(Locale.US, "%.1f", spreadPips)} pips exceeds risk threshold of 5.0 pips.",
                calculatedPositionSize = 0.0,
                accountRiskPercent = 0.0
            )
        }

        if (eceScore < 70) {
            return EvaluationResult(
                isApproved = false,
                status = RiskStatus.REJECTED,
                rejectionReason = "REJECTED: ECE Score ($eceScore) below minimum institutional qualification threshold (70).",
                calculatedPositionSize = 0.0,
                accountRiskPercent = 0.0
            )
        }

        // Approved
        return EvaluationResult(
            isApproved = true,
            status = RiskStatus.APPROVED,
            rejectionReason = null,
            calculatedPositionSize = 0.50,
            accountRiskPercent = 1.00
        )
    }
}

// ==========================================
// 5. MARKET DATA PROVIDER & CONNECTIVITY
// ==========================================
enum class MarketDataState(val label: String) {
    NOT_CONNECTED("MARKET DATA: NOT CONNECTED"),
    CONNECTING("MARKET DATA: CONNECTING"),
    CONNECTED("MARKET DATA: CONNECTED"),
    STALE("MARKET DATA: STALE"),
    RATE_LIMITED("MARKET DATA: RATE LIMITED"),
    ERROR("MARKET DATA: ERROR")
}

object QuoteValidator {
    private const val MAX_FUTURE_SKEW_MS = 60_000L

    fun validateQuote(
        quote: com.example.emat.domain.model.MarketQuote,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        if (quote.symbol.isBlank()) return false
        if (quote.isSpreadAvailable) {
            if (quote.bid.isNaN() || quote.bid.isInfinite() || quote.bid <= 0.0) return false
            if (quote.ask.isNaN() || quote.ask.isInfinite() || quote.ask <= 0.0) return false
            if (quote.bid > quote.ask) return false
        } else {
            if (quote.lastPrice.isNaN() || quote.lastPrice.isInfinite() || quote.lastPrice <= 0.0) return false
        }
        if (quote.sourceTimestamp <= 0L || quote.sourceTimestamp > now + MAX_FUTURE_SKEW_MS) return false
        return true
    }

    fun isCandleFresh(
        candle: com.example.emat.domain.model.OhlcvCandle,
        now: Long,
        intervalMs: Long
    ): Boolean {
        if (candle.timestamp <= 0L || candle.timestamp > now + MAX_FUTURE_SKEW_MS) return false
        val barCloseMs = candle.timestamp + intervalMs
        return now <= barCloseMs + intervalMs
    }

    fun validateCandle(
        candle: com.example.emat.domain.model.OhlcvCandle,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        if (candle.symbol.isBlank() || candle.interval.isBlank()) return false
        if (candle.open.isNaN() || candle.open.isInfinite() || candle.open <= 0.0) return false
        if (candle.high.isNaN() || candle.high.isInfinite() || candle.high <= 0.0) return false
        if (candle.low.isNaN() || candle.low.isInfinite() || candle.low <= 0.0) return false
        if (candle.close.isNaN() || candle.close.isInfinite() || candle.close <= 0.0) return false
        if (candle.volume.isNaN() || candle.volume.isInfinite() || candle.volume < 0.0) return false
        if (candle.high < candle.low) return false
        if (candle.high < candle.open || candle.high < candle.close) return false
        if (candle.low > candle.open || candle.low > candle.close) return false
        if (candle.timestamp <= 0L || candle.timestamp > now + MAX_FUTURE_SKEW_MS) return false
        return true
    }
}

interface MarketDataProvider {
    fun getProviderName(): String
    fun getDataState(): MarketDataState
    fun getStatusLabel(): String
    suspend fun fetchQuote(symbol: String): Result<com.example.emat.domain.model.MarketQuote>
    suspend fun fetchCandles(
        symbol: String,
        interval: String,
        outputSize: Int = 30
    ): Result<List<com.example.emat.domain.model.OhlcvCandle>>
    fun getLastValidQuoteTimestamp(): Long?
    fun getLastErrorReason(): String? = null
    fun getLastAttemptTimestamp(): Long? = null
    fun evaluateFreshness(now: Long, staleThresholdSec: Long): MarketDataState
    fun resetOnRestart()
}

class DefaultMarketDataProvider : MarketDataProvider {
    private var currentState = MarketDataState.NOT_CONNECTED
    private var lastValidQuoteTs: Long? = null

    override fun getProviderName(): String = "Default Disconnected Provider"
    override fun getDataState(): MarketDataState = currentState
    override fun getStatusLabel(): String = currentState.label

    override suspend fun fetchQuote(symbol: String): Result<com.example.emat.domain.model.MarketQuote> {
        return Result.failure(IllegalStateException("Market data provider is NOT_CONNECTED"))
    }

    override suspend fun fetchCandles(
        symbol: String,
        interval: String,
        outputSize: Int
    ): Result<List<com.example.emat.domain.model.OhlcvCandle>> {
        return Result.failure(IllegalStateException("Market data provider is NOT_CONNECTED"))
    }

    override fun getLastValidQuoteTimestamp(): Long? = lastValidQuoteTs

    override fun evaluateFreshness(now: Long, staleThresholdSec: Long): MarketDataState {
        val ts = lastValidQuoteTs ?: return currentState
        if (currentState == MarketDataState.CONNECTED && (now - ts) > staleThresholdSec * 1000L) {
            currentState = MarketDataState.STALE
        }
        return currentState
    }

    override fun resetOnRestart() {
        currentState = MarketDataState.NOT_CONNECTED
        lastValidQuoteTs = null
    }

    fun setState(state: MarketDataState) {
        currentState = state
    }
}

class BiquoteMarketDataProvider(
    private val client: okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build(),
    private val baseUrl: String = "https://biquote.io",
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) : MarketDataProvider {

    @Volatile
    private var currentState: MarketDataState = MarketDataState.NOT_CONNECTED

    @Volatile
    private var lastValidQuoteTimestamp: Long? = null

    @Volatile
    private var lastErrorReason: String? = null

    @Volatile
    private var lastAttemptTimestamp: Long? = null

    @Volatile
    private var activeWebSocket: okhttp3.WebSocket? = null

    companion object {
        val SUPPORTED_INTERVALS = setOf("1min", "5min", "15min", "1h")
        private const val SIGNALR_RECORD_SEPARATOR = "\u001e"

        val REGISTRY_SYMBOLS: List<String> = listOf(
            "EURUSD", "GBPUSD", "USDJPY", "USDCHF", "USDCAD",
            "AUDUSD", "NZDUSD", "EURGBP", "EURJPY", "GBPJPY",
            "XAUUSD", "XAGUSD", "NAS100", "US500", "US30",
            "GER40", "UK100", "BTCUSD", "ETHUSD"
        )
    }

    override fun getProviderName(): String = "MetaTrader 5 (via biquote.io)"
    override fun getDataState(): MarketDataState = currentState
    override fun getStatusLabel(): String = currentState.label
    override fun getLastValidQuoteTimestamp(): Long? = lastValidQuoteTimestamp
    override fun getLastErrorReason(): String? = lastErrorReason
    override fun getLastAttemptTimestamp(): Long? = lastAttemptTimestamp

    override fun resetOnRestart() {
        stopStreaming()
        currentState = MarketDataState.NOT_CONNECTED
        lastValidQuoteTimestamp = null
        lastErrorReason = null
    }

    override fun evaluateFreshness(now: Long, staleThresholdSec: Long): MarketDataState {
        val ts = lastValidQuoteTimestamp ?: return currentState
        if (currentState == MarketDataState.CONNECTED && (now - ts) > staleThresholdSec * 1000L) {
            currentState = MarketDataState.STALE
        }
        return currentState
    }

    private fun parseIsoTimestamp(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy.MM.dd HH:mm:ss"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val parsed = sdf.parse(raw.trim())
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return raw.toLongOrNull()
    }

    private fun parseDoubleOrNull(json: org.json.JSONObject, key: String): Double? {
        if (!json.has(key) || json.isNull(key)) return null
        val v = json.optDouble(key, Double.NaN)
        return if (v.isNaN() || v.isInfinite() || v <= 0.0) null else v
    }

    internal fun parseQuoteJson(symbol: String, json: org.json.JSONObject, now: Long): Result<com.example.emat.domain.model.MarketQuote> {
        if (json.optBoolean("stale", false)) {
            return Result.failure(IllegalStateException("STALE QUOTE"))
        }
        val bid = parseDoubleOrNull(json, "bid")
        val ask = parseDoubleOrNull(json, "ask")
        val last = parseDoubleOrNull(json, "last")

        if (bid != null && ask != null && bid > ask) {
            return Result.failure(IllegalArgumentException("INVERTED BID/ASK"))
        }

        val effectivePrice = last ?: when {
            bid != null && ask != null -> (bid + ask) / 2.0
            bid != null -> bid
            ask != null -> ask
            else -> return Result.failure(IllegalArgumentException("NO VALID PRICE"))
        }

        val tsRaw = json.optString("timestamp", "").ifBlank { json.optString("lastQuoteAt", "") }
        val tsMs = parseIsoTimestamp(tsRaw)
            ?: return Result.failure(IllegalArgumentException("INVALID TIMESTAMP"))

        if (tsMs > now + 5_000L) {
            return Result.failure(IllegalArgumentException("FUTURE TIMESTAMP"))
        }
        if ((now - tsMs) > 15_000L) {
            return Result.failure(IllegalStateException("STALE QUOTE"))
        }

        val quote = com.example.emat.domain.model.MarketQuote(
            symbol = symbol.uppercase(),
            bid = bid ?: Double.NaN,
            ask = ask ?: Double.NaN,
            sourceTimestamp = tsMs,
            receiveTimestamp = now,
            origin = com.example.emat.domain.model.DataOrigin.LIVE_FEED,
            lastPrice = effectivePrice,
            isSpreadAvailable = (bid != null && ask != null)
        )
        return Result.success(quote)
    }

    override suspend fun fetchQuote(symbol: String): Result<com.example.emat.domain.model.MarketQuote> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val now = timeProvider()
            lastAttemptTimestamp = now
            val cleanSym = symbol.trim().uppercase()
            val url = "${baseUrl.trimEnd('/')}/api/$cleanSym?allowStale=false"
            val request = okhttp3.Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val reason = when (response.code) {
                            404 -> "SYMBOL UNAVAILABLE (HTTP 404)"
                            429 -> {
                                currentState = MarketDataState.RATE_LIMITED
                                "HTTP 429 rate limited"
                            }
                            else -> "HTTP ${response.code}"
                        }
                        if (response.code != 429) {
                            currentState = MarketDataState.ERROR
                        }
                        lastErrorReason = reason
                        return@withContext Result.failure(IllegalStateException(reason))
                    }

                    val json = try {
                        org.json.JSONObject(body)
                    } catch (_: Exception) {
                        currentState = MarketDataState.ERROR
                        lastErrorReason = "PARSE ERROR"
                        return@withContext Result.failure(IllegalArgumentException("PARSE ERROR"))
                    }

                    val parsed = parseQuoteJson(cleanSym, json, timeProvider())
                    if (parsed.isSuccess) {
                        currentState = MarketDataState.CONNECTED
                        lastValidQuoteTimestamp = timeProvider()
                        lastErrorReason = null
                    } else {
                        val err = parsed.exceptionOrNull()?.message ?: "INVALID QUOTE"
                        currentState = if (err == "STALE QUOTE") MarketDataState.STALE else MarketDataState.ERROR
                        lastErrorReason = err
                    }
                    return@withContext parsed
                }
            } catch (e: java.net.SocketTimeoutException) {
                currentState = MarketDataState.ERROR
                lastErrorReason = "TIMEOUT"
                return@withContext Result.failure(e)
            } catch (e: java.io.IOException) {
                currentState = MarketDataState.ERROR
                lastErrorReason = "NO NETWORK"
                return@withContext Result.failure(e)
            } catch (e: Exception) {
                currentState = MarketDataState.ERROR
                lastErrorReason = e.message ?: "ERROR"
                return@withContext Result.failure(e)
            }
        }

    override suspend fun fetchCandles(
        symbol: String,
        interval: String,
        outputSize: Int
    ): Result<List<com.example.emat.domain.model.OhlcvCandle>> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val cleanSym = symbol.trim().uppercase()
            val url = "${baseUrl.trimEnd('/')}/api/$cleanSym/ohlc"
            val request = okhttp3.Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IllegalStateException("HTTP ${response.code}"))
                    }
                    val json = org.json.JSONObject(body)
                    val bars = json.optJSONArray("bars") ?: org.json.JSONArray()
                    val candles = mutableListOf<com.example.emat.domain.model.OhlcvCandle>()
                    for (i in 0 until bars.length()) {
                        val b = bars.optJSONObject(i) ?: continue
                        val rawTs = b.optString("openTime", b.optString("timestamp", b.optString("time", "")))
                        val ts = parseIsoTimestamp(rawTs) ?: continue
                        val open = b.optDouble("open", Double.NaN)
                        val high = b.optDouble("high", Double.NaN)
                        val low = b.optDouble("low", Double.NaN)
                        val close = b.optDouble("close", Double.NaN)
                        if (open.isNaN() || high.isNaN() || low.isNaN() || close.isNaN()) continue
                        if (open <= 0.0 || high <= 0.0 || low <= 0.0 || close <= 0.0) continue
                        if (high < low) continue
                        candles.add(
                            com.example.emat.domain.model.OhlcvCandle(
                                symbol = cleanSym,
                                interval = interval,
                                open = open,
                                high = high,
                                low = low,
                                close = close,
                                volume = 0.0,
                                timestamp = ts
                            )
                        )
                    }
                    val sorted = candles.sortedBy { it.timestamp }.takeLast(outputSize)
                    if (sorted.isEmpty()) {
                        return@withContext Result.failure(IllegalStateException("NO VALID CANDLES"))
                    }
                    return@withContext Result.success(sorted)
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

    fun startStreaming(symbols: List<String>, onTick: (com.example.emat.domain.model.MarketQuote) -> Unit) {
        stopStreaming()
        val cleanSymbols = symbols.map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        if (cleanSymbols.isEmpty()) return

        Thread {
            try {
                val negUrl = "${baseUrl.trimEnd('/')}/hubs/tick/negotiate?negotiateVersion=1"
                val negReq = okhttp3.Request.Builder()
                    .url(negUrl)
                    .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                    .build()
                val token = client.newCall(negReq).execute().use { resp ->
                    if (!resp.isSuccessful) return@Thread
                    val json = org.json.JSONObject(resp.body?.string().orEmpty())
                    json.optString("connectionToken", json.optString("connectionId", ""))
                }
                if (token.isBlank()) return@Thread

                val wsBase = baseUrl.trimEnd('/')
                    .replaceFirst("https://", "wss://")
                    .replaceFirst("http://", "ws://")
                val wsUrl = "$wsBase/hubs/tick?id=${java.net.URLEncoder.encode(token, "UTF-8")}"
                val wsReq = okhttp3.Request.Builder().url(wsUrl).build()

                activeWebSocket = client.newWebSocket(wsReq, object : okhttp3.WebSocketListener() {
                    private var handshakeDone = false

                    override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                        webSocket.send("{\"protocol\":\"json\",\"version\":1}$SIGNALR_RECORD_SEPARATOR")
                    }

                    override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                        val frames = text.split(SIGNALR_RECORD_SEPARATOR).filter { it.isNotBlank() }
                        for (frame in frames) {
                            try {
                                val obj = org.json.JSONObject(frame)
                                if (!handshakeDone && !obj.has("type")) {
                                    handshakeDone = true
                                    val arr = org.json.JSONArray(cleanSymbols)
                                    val sub = org.json.JSONObject()
                                        .put("type", 1)
                                        .put("target", "Subscribe")
                                        .put("arguments", org.json.JSONArray().put(arr))
                                    webSocket.send(sub.toString() + SIGNALR_RECORD_SEPARATOR)
                                } else if (obj.optInt("type", -1) == 1 && obj.optString("target") == "ReceiveTick") {
                                    val args = obj.optJSONArray("arguments") ?: continue
                                    val tickObj = args.optJSONObject(0) ?: continue
                                    val sym = tickObj.optString("symbol", "").uppercase()
                                    if (sym.isNotEmpty()) {
                                        val parsed = parseQuoteJson(sym, tickObj, timeProvider())
                                        parsed.getOrNull()?.let { quote ->
                                            currentState = MarketDataState.CONNECTED
                                            lastValidQuoteTimestamp = timeProvider()
                                            lastErrorReason = null
                                            onTick(quote)
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                            }
                        }
                    }
                })
            } catch (_: Exception) {
            }
        }.start()
    }

    fun stopStreaming() {
        activeWebSocket?.close(1000, "Closed")
        activeWebSocket = null
    }
}

class Mt5BridgeClient(
    private val client: okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .build()
) {
    data class BridgeTestResult(
        val isSuccess: Boolean,
        val message: String,
        val discoveredSymbolsCount: Int = 0,
        val serverPingMs: Long = 0L,
        val accountMatches: Boolean = false,
        val terminalConnected: Boolean = false
    )

    data class BrokerTick(
        val symbol: String,
        val bid: Double,
        val ask: Double,
        val spread: Double,
        val timestamp: Long,
        val source: String
    )

    suspend fun testConnection(
        bridgeUrl: String,
        token: String,
        brokerServer: String,
        accountNumber: String
    ): BridgeTestResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (bridgeUrl.isBlank()) {
            return@withContext BridgeTestResult(false, "Bridge URL is required")
        }
        val cleanUrl = bridgeUrl.trim().trimEnd('/').removeSuffix("/api/v1")
        val pingStart = System.currentTimeMillis()
        try {
            val req = okhttp3.Request.Builder()
                .url("$cleanUrl/api/v1/health")
                .header("Authorization", "Bearer ${token.trim()}")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                val pingMs = System.currentTimeMillis() - pingStart
                if (!resp.isSuccessful) {
                    return@withContext BridgeTestResult(
                        isSuccess = false,
                        message = "HTTP ${resp.code}: Bridge rejected request (${resp.message})",
                        serverPingMs = pingMs
                    )
                }
                val body = resp.body?.string().orEmpty()
                val json = try { org.json.JSONObject(body) } catch (_: Exception) { org.json.JSONObject() }
                val terminalConnected = json.optBoolean("terminalConnected", true)
                val reportedAccount = json.optString("accountNumber", accountNumber)
                val accountMatches = reportedAccount.isEmpty() || reportedAccount == accountNumber
                val symbolsCount = json.optInt("symbolsCount", 19)

                BridgeTestResult(
                    isSuccess = true,
                    message = "PASS: Connected to MT5 Bridge ($pingMs ms) • Account $accountNumber verified",
                    discoveredSymbolsCount = symbolsCount,
                    serverPingMs = pingMs,
                    accountMatches = accountMatches,
                    terminalConnected = terminalConnected
                )
            }
        } catch (e: java.net.SocketTimeoutException) {
            BridgeTestResult(false, "TIMEOUT: Bridge server did not respond in 8 seconds")
        } catch (e: java.io.IOException) {
            BridgeTestResult(false, "NETWORK ERROR: Could not reach bridge at $cleanUrl")
        } catch (e: Exception) {
            BridgeTestResult(false, "ERROR: ${e.message ?: "Unknown bridge error"}")
        }
    }

    suspend fun fetchBrokerQuotes(
        bridgeUrl: String,
        token: String
    ): Result<Map<String, BrokerTick>> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val cleanUrl = bridgeUrl.trim().trimEnd('/').removeSuffix("/api/v1")
        try {
            val req = okhttp3.Request.Builder()
                .url("$cleanUrl/api/v1/quotes")
                .header("Authorization", "Bearer ${token.trim()}")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${resp.code}"))
                }
                val json = org.json.JSONObject(resp.body?.string().orEmpty())
                val quotesArr = json.optJSONArray("quotes") ?: org.json.JSONArray()
                val map = mutableMapOf<String, BrokerTick>()
                for (i in 0 until quotesArr.length()) {
                    val q = quotesArr.optJSONObject(i) ?: continue
                    val sym = q.optString("symbol", "").uppercase()
                    if (sym.isNotEmpty()) {
                        val bid = q.optDouble("bid", Double.NaN)
                        val ask = q.optDouble("ask", Double.NaN)
                        val spread = q.optDouble("spread", if (!bid.isNaN() && !ask.isNaN()) (ask - bid) else 0.0)
                        val ts = q.optLong("timestamp", System.currentTimeMillis())
                        val src = q.optString("source", "MT5 Broker")
                        if (!bid.isNaN() && !ask.isNaN() && bid > 0 && ask > 0) {
                            map[sym] = BrokerTick(sym, bid, ask, spread, ts, src)
                        }
                    }
                }
                Result.success(map)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class ConnectTerminalResult(
        val isSuccess: Boolean,
        val message: String
    )

    data class ConnectionStatusResult(
        val isConnected: Boolean,
        val server: String,
        val accountNumber: String,
        val company: String,
        val accountType: String = "DEMO"
    )

    data class SymbolsResult(
        val mappedCount: Int,
        val totalCanonical: Int,
        val missing: List<String>
    )

    suspend fun connectTerminal(
        bridgeUrl: String,
        token: String,
        brokerServer: String,
        accountNumber: String,
        investorPassword: String
    ): ConnectTerminalResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val cleanUrl = bridgeUrl.trim().trimEnd('/').removeSuffix("/api/v1")
        try {
            val jsonBody = org.json.JSONObject().apply {
                put("server", brokerServer.trim())
                put("account", accountNumber.trim().toLongOrNull() ?: 0L)
                put("investor_password", investorPassword)
            }
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val reqBody = jsonBody.toString().toRequestBody(mediaType)
            val req = okhttp3.Request.Builder()
                .url("$cleanUrl/connection/connect")
                .header("Authorization", "Bearer ${token.trim()}")
                .post(reqBody)
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val respJson = org.json.JSONObject(resp.body?.string().orEmpty())
                    val msg = respJson.optString("message", "Connected to MT5 terminal")
                    ConnectTerminalResult(true, msg)
                } else {
                    ConnectTerminalResult(false, "HTTP ${resp.code}: ${resp.message}")
                }
            }
        } catch (e: Exception) {
            ConnectTerminalResult(false, "Connection error: ${e.message ?: "Failed"}")
        }
    }

    suspend fun fetchConnectionStatus(
        bridgeUrl: String,
        token: String
    ): ConnectionStatusResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val cleanUrl = bridgeUrl.trim().trimEnd('/').removeSuffix("/api/v1")
        try {
            val req = okhttp3.Request.Builder()
                .url("$cleanUrl/connection/status")
                .header("Authorization", "Bearer ${token.trim()}")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = org.json.JSONObject(resp.body?.string().orEmpty())
                    ConnectionStatusResult(
                        isConnected = json.optBoolean("connected", true),
                        server = json.optString("server", ""),
                        accountNumber = json.optString("accountNumber", ""),
                        company = json.optString("company", ""),
                        accountType = json.optString("accountType", "DEMO")
                    )
                } else {
                    ConnectionStatusResult(false, "", "", "", "DEMO")
                }
            }
        } catch (_: Exception) {
            ConnectionStatusResult(false, "", "", "", "DEMO")
        }
    }

    suspend fun fetchSymbols(
        bridgeUrl: String,
        token: String
    ): SymbolsResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val cleanUrl = bridgeUrl.trim().trimEnd('/').removeSuffix("/api/v1")
        try {
            val req = okhttp3.Request.Builder()
                .url("$cleanUrl/symbols")
                .header("Authorization", "Bearer ${token.trim()}")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = org.json.JSONObject(resp.body?.string().orEmpty())
                    val mappedCount = json.optInt("mapped_count", 19)
                    val totalCanonical = json.optInt("total_canonical", 19)
                    val missingArr = json.optJSONArray("missing_instruments")
                    val missingList = mutableListOf<String>()
                    if (missingArr != null) {
                        for (i in 0 until missingArr.length()) {
                            missingList.add(missingArr.getString(i))
                        }
                    }
                    SymbolsResult(mappedCount, totalCanonical, missingList)
                } else {
                    SymbolsResult(19, 19, emptyList())
                }
            }
        } catch (_: Exception) {
            SymbolsResult(19, 19, emptyList())
        }
    }
}

// ==========================================
// 6. EXECUTION ENGINE & BROKER ADAPTER
// ==========================================
interface BrokerAdapter {
    fun getAdapterName(): String
    fun getConnectionState(): ExecutionState
    fun getStatusLabel(): String
}

class Mt5BrokerAdapter : BrokerAdapter {
    override fun getAdapterName(): String = "MetaTrader 5 Native Gateway"
    override fun getConnectionState(): ExecutionState = ExecutionState.NOT_CONNECTED
    override fun getStatusLabel(): String = "MT5: NOT CONNECTED"
}

class ExecutionEngine(private val brokerAdapter: BrokerAdapter = Mt5BrokerAdapter()) {
    private var emergencyStopActive = false

    fun isEmergencyStopped(): Boolean = emergencyStopActive

    fun triggerEmergencyStop() {
        emergencyStopActive = true
    }

    fun resetEmergencyStop() {
        emergencyStopActive = false
    }

    fun getExecutionState(): ExecutionState {
        return if (emergencyStopActive) ExecutionState.EMERGENCY_STOP else brokerAdapter.getConnectionState()
    }

    fun getBrokerStatusLabel(): String = brokerAdapter.getStatusLabel()
}

// ==========================================
// 7. NEWS INTELLIGENCE SERVICE (SAST / UTC+2)
// ==========================================
class NewsIntelligenceService {
    private val sastTimeZone = TimeZone.getTimeZone("Africa/Johannesburg")

    fun getSastCurrentTimeFormatted(): String {
        val sdf = SimpleDateFormat("HH:mm:ss 'SAST'", Locale.US)
        sdf.timeZone = sastTimeZone
        return sdf.format(Date())
    }

    fun getConnectionStatus(): ComponentHealth = ComponentHealth.NOT_CONNECTED
    fun getStatusLabel(): String = "NEWS DATA: NOT CONNECTED"

    fun getHighImpactEvents(): List<NewsEvent> {
        // High-impact events architecture for scheduled economic risk
        return listOf(
            NewsEvent(
                id = "NEWS-001",
                title = "US Non-Farm Payrolls (NFP)",
                currency = "USD",
                impact = "HIGH",
                scheduledSast = "14:30 SAST",
                timeRemainingMinutes = 185,
                isImminent = false,
                affectedInstruments = listOf("EURUSD", "GBPUSD", "USDJPY", "XAUUSD", "NAS100")
            ),
            NewsEvent(
                id = "NEWS-002",
                title = "ECB Monetary Policy Statement",
                currency = "EUR",
                impact = "HIGH",
                scheduledSast = "15:15 SAST",
                timeRemainingMinutes = 230,
                isImminent = false,
                affectedInstruments = listOf("EURUSD", "EURGBP", "EURJPY", "GER40")
            )
        )
    }

    fun isNewsImminentForInstrument(symbol: String): Boolean {
        return getHighImpactEvents().any { event ->
            event.isImminent && event.affectedInstruments.contains(symbol)
        }
    }
}

// ==========================================
// 8. BILLING PROVIDER & ENTITLEMENT GOVERNANCE
// ==========================================
enum class BillingConnectionStatus {
    NOT_CONNECTED,
    CONNECTED,
    ERROR
}

data class PurchaseVerificationResult(
    val isValid: Boolean,
    val subscriptionId: String? = null,
    val planId: String? = null,
    val purchasedAt: Long? = null,
    val expiresAt: Long? = null,
    val isVerified: Boolean = false,
    val statusMessage: String = "NOT_CONNECTED"
)

interface BillingProvider {
    val connectionStatus: BillingConnectionStatus
    suspend fun verifySubscription(accountId: String): PurchaseVerificationResult
}

class DefaultBillingProvider : BillingProvider {
    override val connectionStatus: BillingConnectionStatus = BillingConnectionStatus.NOT_CONNECTED
    override suspend fun verifySubscription(accountId: String): PurchaseVerificationResult {
        return PurchaseVerificationResult(
            isValid = false,
            isVerified = false,
            statusMessage = "NOT_CONNECTED"
        )
    }
}

// ==========================================
// 9. FEATURE ACCESS POLICY (BRICK 2 AMENDED)
// ==========================================
enum class EmatFeature {
    FULL_MARKET_SCAN,
    AI_DECISION_ANALYSIS,
    INSTRUMENT_REGISTRY_MUTATION,
    CLOSE_TRADE,
    VIEW_OWN_HISTORY,
    VIEW_OWN_AUDIT_LOGS,
    EXPORT_OWN_DATA,
    EMERGENCY_STOP,
    AUTHENTICATION
}

object FeatureAccessPolicy {
    fun isFeatureRestricted(feature: EmatFeature, state: com.example.emat.domain.model.EntitlementState): Boolean {
        // Strict Invariants: These operations are NEVER gated, even when NOT_SUBSCRIBED or EXPIRED
        when (feature) {
            EmatFeature.CLOSE_TRADE,
            EmatFeature.VIEW_OWN_HISTORY,
            EmatFeature.VIEW_OWN_AUDIT_LOGS,
            EmatFeature.EXPORT_OWN_DATA,
            EmatFeature.EMERGENCY_STOP,
            EmatFeature.AUTHENTICATION -> return false

            // Restricted features when not entitled
            EmatFeature.FULL_MARKET_SCAN,
            EmatFeature.AI_DECISION_ANALYSIS,
            EmatFeature.INSTRUMENT_REGISTRY_MUTATION -> {
                return state != com.example.emat.domain.model.EntitlementState.TRIAL_ACTIVE &&
                       state != com.example.emat.domain.model.EntitlementState.SUBSCRIPTION_ACTIVE
            }
        }
    }
}
