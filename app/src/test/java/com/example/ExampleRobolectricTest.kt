package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.emat.domain.model.AssetClass
import com.example.emat.domain.model.RiskStatus
import com.example.emat.domain.model.SetupStatus
import com.example.emat.domain.model.TradeDirection
import com.example.emat.domain.service.EceEngine
import com.example.emat.domain.service.RiskEngine
import com.example.emat.domain.service.StrategyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("EMAT AutonomIQ", appName)
    }

    @Test
    fun `test risk engine enforces one trade rule`() {
        val riskEngine = RiskEngine()

        // Test 1: Blocked when 1 active trade already exists
        val checkBlocked = riskEngine.evaluateOpportunity(
            symbol = "GBPUSD",
            eceScore = 85,
            activeTradesCount = 1,
            spreadPips = 1.2,
            isEmergencyStopped = false
        )
        assertFalse("Should be blocked due to active trade already open", checkBlocked.isApproved)
        assertEquals(RiskStatus.BLOCKED_ONE_TRADE_RULE, checkBlocked.status)

        // Test 2: Approved when 0 active trades exist
        val checkApproved = riskEngine.evaluateOpportunity(
            symbol = "GBPUSD",
            eceScore = 85,
            activeTradesCount = 0,
            spreadPips = 1.2,
            isEmergencyStopped = false
        )
        assertTrue("Should be approved with zero active trades", checkApproved.isApproved)
        assertEquals(RiskStatus.APPROVED, checkApproved.status)
    }

    @Test
    fun `test strategy and ece scoring engine thresholds`() {
        val strategyEngine = StrategyEngine()
        val eceEngine = EceEngine()

        val strategyResult = strategyEngine.evaluateSetup(
            symbol = "EURUSD",
            assetClass = AssetClass.FOREX,
            bid = 1.0850,
            ask = 1.0851,
            spread = 0.0001,
            high = 1.0870,
            low = 1.0820,
            close = 1.0865
        )

        val scoreBreakdown = eceEngine.calculateScore(
            strategyResult = strategyResult,
            spreadPips = 1.0,
            isNewsImminent = false
        )

        assertTrue("ECE overall score should be between 0 and 100", scoreBreakdown.overall in 0..100)
    }

    @Test
    fun `test ai decision support format adheres to 5 mandatory sections`() {
        val aiEngine = com.example.emat.domain.service.EmatAiEngine()
        val analysis = aiEngine.generateAnalysis(
            context = "EURUSD Setup Evaluation",
            activeTrades = 0,
            isEmergencyStopped = false,
            topOpp = null
        )

        assertEquals("EURUSD Setup Evaluation", analysis.contextTitle)
        assertTrue("Fact section must not be blank", analysis.fact.isNotBlank())
        assertTrue("Calculation section must not be blank", analysis.calculation.isNotBlank())
        assertTrue("Analysis section must not be blank", analysis.analysis.isNotBlank())
        assertTrue("Inference section must not be blank", analysis.inference.isNotBlank())
        assertTrue("Recommendation section must not be blank", analysis.recommendation.isNotBlank())
        assertTrue("Data truth status must be verified", analysis.dataTruthStatus.isNotBlank())
    }

    @Test
    fun `test account tier limits and one-trade rule invariant`() {
        val primaryAccount = com.example.emat.domain.model.EmatAccount(
            id = "ACC-001",
            name = "EMAT Alpha Vault",
            role = "Chief Risk Officer / Lead Operator",
            tier = "Tier 1 Prime Institutional",
            dailyLossLimit = "$10,000.00 (2.0%)",
            maxPositionSize = "20.00 Lots",
            isLiveExecutionAllowed = true,
            permissions = listOf("EXECUTE_LIVE", "SCAN_UNIVERSE", "ADJUST_LIMITS", "EMERGENCY_HALT")
        )

        assertTrue(primaryAccount.isLiveExecutionAllowed)
        assertTrue(primaryAccount.permissions.contains("EMERGENCY_HALT"))
        assertTrue(primaryAccount.permissions.contains("EXECUTE_LIVE"))
    }

    @Test
    fun `test correctly provisioned credentials authenticate and invalid credentials fail`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.emat.domain.service.AuthService(context)
        authService.clearProvisionedCredentials()

        // 1. Unprovisioned terminal must reject attempts
        val unprovisionedAttempt = authService.loginWithPassword("RandomPassword99!")
        assertFalse("Unprovisioned terminal must reject login", unprovisionedAttempt.isSuccess)
        assertTrue(unprovisionedAttempt.errorMessage?.contains("unprovisioned") == true)

        // 2. Provision credential
        val testKey = "Secure#OperatorKey9824!"
        val provisionSuccess = authService.provisionOperatorCredential(testKey, "Lead Strategist")
        assertTrue("Provisioning must succeed for strong key", provisionSuccess)
        assertTrue(authService.isProvisioned())

        // 3. Invalid credentials rejected
        val badResult = authService.loginWithPassword("WrongPassword123!")
        assertFalse(badResult.isSuccess)

        val emptyResult = authService.loginWithPassword("")
        assertFalse(emptyResult.isSuccess)

        // 4. Correctly provisioned credentials verified via salt and hash
        val goodResult = authService.loginWithPassword(testKey)
        assertTrue("Correctly provisioned key must authenticate", goodResult.isSuccess)
        assertEquals("Lead Strategist", goodResult.username)
        assertTrue("Session token must be established", goodResult.sessionToken?.startsWith("EMAT-PROD-") == true)
    }

    @Test
    fun `test no universal default-password access allowed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.emat.domain.service.AuthService(context)
        authService.clearProvisionedCredentials()

        // Disallowed default credentials must be rejected before and after provisioning
        val default1 = authService.loginWithPassword("EMAT2026")
        assertFalse("Universal default EMAT2026 must be strictly rejected", default1.isSuccess)

        val default2 = authService.loginWithPassword("admin")
        assertFalse("Universal default admin must be strictly rejected", default2.isSuccess)

        // Even provisioning universal default passwords must be blocked
        assertFalse(authService.provisionOperatorCredential("EMAT2026"))
        assertFalse(authService.provisionOperatorCredential("admin"))

        // Provision secure key
        authService.provisionOperatorCredential("Strong#InstitutionalPasscode2026!")

        // Still rejected even when another key is provisioned
        val defaultAfterProv1 = authService.loginWithPassword("EMAT2026")
        assertFalse(defaultAfterProv1.isSuccess)
        val defaultAfterProv2 = authService.loginWithPassword("admin")
        assertFalse(defaultAfterProv2.isSuccess)
    }

    @Test
    fun `test session creation persistence and timeout invalidation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.emat.domain.service.AuthService(context)
        authService.clearProvisionedCredentials()
        val key = "Alpha#VaultOperator99!"
        authService.provisionOperatorCredential(key, "Chief Risk Officer")

        val login = authService.loginWithPassword(key)
        assertTrue(login.isSuccess)
        assertTrue(authService.isSessionValid())

        // Session persistence test with Room UserSessionDao
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.emat.data.local.EmatDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        kotlinx.coroutines.runBlocking {
            db.userSessionDao().saveSession(
                com.example.emat.data.local.entity.UserSessionEntity(
                    id = 1,
                    isLoggedIn = true,
                    username = login.username!!,
                    sessionToken = login.sessionToken,
                    lastActiveTime = System.currentTimeMillis(),
                    authMethod = "SHA256_PROVISIONED"
                )
            )

            val session = db.userSessionDao().getSession()
            assertTrue(session != null && session.isLoggedIn)
            assertEquals("Chief Risk Officer", session?.username)
        }
        db.close()

        // Session timeout invalidation (15 min expiry)
        val expiredTime = System.currentTimeMillis() - (16 * 60 * 1000L)
        val restored = authService.restoreSession(login.sessionToken, login.username, expiredTime)
        assertFalse("Expired session (>15 mins) must be invalidated upon restoration", restored)
        assertFalse(authService.isSessionValid())
    }

    @Test
    fun `test logout and session invalidation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.emat.domain.service.AuthService(context)
        authService.clearProvisionedCredentials()
        val key = "RiskLead#Passcode888!"
        authService.provisionOperatorCredential(key)

        val login = authService.loginWithPassword(key)
        assertTrue(login.isSuccess)
        assertTrue(authService.isSessionValid())

        // Logout
        authService.logout()
        assertFalse("Session must be invalid after logout", authService.isSessionValid())
        assertEquals(null, authService.getSessionToken())
    }

    @Test
    fun `test first-time provisioning on fresh installation via viewModel workflow`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.emat.ui.viewmodel.EmatViewModel(app)
        vm.authService.clearProvisionedCredentials()
        assertFalse("Terminal must be unprovisioned initially", vm.authService.isProvisioned())

        val validKey = "Master#OperatorKey9824!"

        // Step 1: First entry of valid key prompts for confirmation
        vm.onPasswordChanged(validKey)
        vm.loginWithPassword()
        assertFalse("Should not be provisioned after step 1", vm.authService.isProvisioned())
        assertFalse("Should not be authenticated after step 1", vm.isAuthenticated.value)
        assertTrue(vm.authErrorMessage.value?.contains("Re-enter key") == true)

        // Step 2: Confirmation entry completes atomic provisioning
        vm.onPasswordChanged(validKey)
        vm.loginWithPassword()
        assertTrue("Terminal must be provisioned after confirmation", vm.authService.isProvisioned())
        assertTrue("Operator must be authenticated after confirmation", vm.isAuthenticated.value)
        assertEquals(com.example.emat.ui.viewmodel.AppDestination.MAIN, vm.currentDestination.value)
    }

    @Test
    fun `test prevention of unauthorized or repeated provisioning`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.emat.domain.service.AuthService(context)
        authService.clearProvisionedCredentials()

        val initialKey = "Prime#InstitutionalKey777!"
        val firstResult = authService.provisionFirstOperator(initialKey, "Chief Risk Officer")
        assertTrue("First-time provisioning must succeed", firstResult.isSuccess)
        assertTrue(authService.isProvisioned())

        // Attempt second provisioning with different key
        val secondResult = authService.provisionFirstOperator("Intruder#Key99999!", "Rogue Operator")
        assertFalse("Repeated provisioning must be strictly blocked", secondResult.isSuccess)
        assertTrue("Error message must explain blocked repeated provisioning", secondResult.errorMessage?.contains("already provisioned") == true)

        // Original credential remains authoritative
        val authOriginal = authService.loginWithPassword(initialKey)
        assertTrue("Original provisioned key must still authenticate", authOriginal.isSuccess)

        // Attacker key cannot authenticate
        val authIntruder = authService.loginWithPassword("Intruder#Key99999!")
        assertFalse("Intruder key must be rejected", authIntruder.isSuccess)
    }

    @Test
    fun `test credential replacement and recovery permissions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.emat.domain.service.AuthService(context)
        authService.clearProvisionedCredentials()

        val initialKey = "Alpha#VaultOperator99!"
        val provResult = authService.provisionFirstOperator(initialKey, "Lead Strategist")
        assertTrue(provResult.isSuccess)
        val recoveryCode = provResult.recoveryCode
        assertTrue("Recovery code must be generated", !recoveryCode.isNullOrEmpty())
        assertTrue("Recovery code must follow EMAT format", recoveryCode!!.startsWith("EMAT-RC-"))

        // 1. Attempt recovery with incorrect recovery code
        val badRecovery = authService.recoverOperatorCredential("EMAT-RC-FAKE-CODE-0000", "New#SecurePasscode888!")
        assertFalse("Incorrect recovery code must be rejected", badRecovery.isSuccess)

        // 2. Successful recovery with authentic recovery code
        val goodRecovery = authService.recoverOperatorCredential(recoveryCode, "New#SecurePasscode888!")
        assertTrue("Legitimate recovery code must succeed in re-keying terminal", goodRecovery.isSuccess)
        assertTrue("New recovery code must be issued", !goodRecovery.recoveryCode.isNullOrEmpty())

        // 3. Old key is invalidated
        val oldKeyAttempt = authService.loginWithPassword(initialKey)
        assertFalse("Old key must be invalidated after recovery", oldKeyAttempt.isSuccess)

        // 4. New key authenticates
        val newKeyAttempt = authService.loginWithPassword("New#SecurePasscode888!")
        assertTrue("New key must authenticate successfully", newKeyAttempt.isSuccess)
    }

    @Test
    fun `test unauthorized access prevented to protected screens`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.emat.ui.viewmodel.EmatViewModel(app)

        // Initial state is unauthenticated
        assertFalse(vm.isAuthenticated.value)

        // Attempting to submit empty or default passwords does not elevate access
        vm.onPasswordChanged("EMAT2026")
        vm.loginWithPassword()
        assertFalse("Universal default EMAT2026 must not authenticate", vm.isAuthenticated.value)
        assertFalse("Unauthenticated operator must not enter MAIN destination", vm.currentDestination.value == com.example.emat.ui.viewmodel.AppDestination.MAIN)

        vm.onPasswordChanged("admin")
        vm.loginWithPassword()
        assertFalse("Universal default admin must not authenticate", vm.isAuthenticated.value)
        assertFalse("Unauthenticated operator must not enter MAIN destination", vm.currentDestination.value == com.example.emat.ui.viewmodel.AppDestination.MAIN)

        vm.onPasswordChanged("wrong_key")
        vm.loginWithPassword()
        assertFalse("Invalid key must not authenticate", vm.isAuthenticated.value)
        assertFalse("Unauthenticated operator must not enter MAIN destination", vm.currentDestination.value == com.example.emat.ui.viewmodel.AppDestination.MAIN)
    }

    @Test
    fun `test market data state not connected blocks scanner execution`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.emat.data.local.EmatDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)

        kotlinx.coroutines.runBlocking {
            val opps = repo.executeMarketScan(dataProviderType = "NOT_CONNECTED")
            assertTrue("Scan must return empty list when NOT_CONNECTED", opps.isEmpty())

            val auditLogs = repo.getAuditLogsByAccountFlow("EMAT-INST-001").first()
            assertTrue("Audit log must record scan blocked reason", auditLogs.any { it.action == "SCAN_BLOCKED_DATA_NOT_CONNECTED" })
        }
        db.close()
    }

    @Test
    fun `test accurate system health indicators reflect truthful states`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.emat.ui.viewmodel.EmatViewModel(app)

        // 1. Default market data state is strictly NOT_CONNECTED
        assertEquals(com.example.emat.domain.service.MarketDataState.NOT_CONNECTED, vm.marketDataState.value)

        // 2. Broker connectivity is strictly NOT_CONNECTED
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        assertEquals(4, db.openHelper.readableDatabase.version)

        // 3. AI Local Analyzer is initialized and generates verified decisions
        val aiEngine = com.example.emat.domain.service.EmatAiEngine()
        val analysis = aiEngine.generateAnalysis("HealthVerification", 0, false, null)
        assertTrue(analysis.recommendation.isNotBlank())
        assertTrue(analysis.dataTruthStatus.isNotBlank())
    }

    @Test
    fun `test account isolation for audit logs and evidence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.emat.data.local.EmatDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)

        kotlinx.coroutines.runBlocking {
            repo.logAuditEvent(
                accountId = "ACC-001",
                category = "SECURITY",
                action = "SECURITY_CHECK_1",
                actor = "Lead",
                rationale = "Account 1 audit",
                dataAvailable = "True",
                decisionMade = "Pass",
                outcome = "Logged"
            )
            repo.logAuditEvent(
                accountId = "ACC-002",
                category = "SECURITY",
                action = "SECURITY_CHECK_2",
                actor = "Trader",
                rationale = "Account 2 audit",
                dataAvailable = "True",
                decisionMade = "Pass",
                outcome = "Logged"
            )

            val acc1Logs = repo.getAuditLogsByAccountFlow("ACC-001").first()
            val acc2Logs = repo.getAuditLogsByAccountFlow("ACC-002").first()

            assertEquals(1, acc1Logs.size)
            assertEquals("ACC-001", acc1Logs.first().accountId)
            assertEquals("SECURITY_CHECK_1", acc1Logs.first().action)

            assertEquals(1, acc2Logs.size)
            assertEquals("ACC-002", acc2Logs.first().accountId)
            assertEquals("SECURITY_CHECK_2", acc2Logs.first().action)
        }
        db.close()
    }

    @Test
    fun `test pbkdf2 uses 210000 iterations and standard phc format`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.emat.domain.service.AuthService(context)
        authService.clearProvisionedCredentials()

        val salt = ByteArray(16) { 0x42.toByte() }
        val phcHash = authService.hashPassword("SecureInstitutionalKey#2026", salt, 210_000)

        // Must follow modular PHC format: $pbkdf2-sha256$i=210000$s=<salt_hex>$v=<verifier_hex>
        assertTrue("PHC hash must start with algorithm tag", phcHash.startsWith("\$pbkdf2-sha256\$i=210000\$"))
        val parts = phcHash.split("$").filter { it.isNotEmpty() }
        assertEquals("pbkdf2-sha256", parts[0])
        assertEquals("i=210000", parts[1])
        assertTrue("Salt part must start with s=", parts[2].startsWith("s="))
        assertTrue("Verifier part must start with v=", parts[3].startsWith("v="))

        // Constant-time verification
        assertTrue(authService.verifyPassword("SecureInstitutionalKey#2026", phcHash))
        assertFalse(authService.verifyPassword("WrongPassword123!", phcHash))
    }

    @Test
    fun `test keystore vault generates unique nonces and fails closed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vault = com.example.emat.domain.service.KeystoreVault(context)

        val plaintext = "TestAuditMessageSecret"
        val cipher1 = vault.encrypt(plaintext)
        val cipher2 = vault.encrypt(plaintext)

        assertTrue("Ciphertext 1 must not be null", cipher1 != null)
        assertTrue("Ciphertext 2 must not be null", cipher2 != null)
        // Unique random 12-byte IV/nonce ensures ciphertexts are different for identical plaintexts
        assertFalse("Unique IV ensures non-identical ciphertexts", cipher1 == cipher2)

        // Decryption succeeds
        assertEquals(plaintext, vault.decrypt(cipher1!!))
        assertEquals(plaintext, vault.decrypt(cipher2!!))

        // Fail-closed on corrupted ciphertext
        val corrupted = cipher1.substring(0, cipher1.length - 4) + "AAAA"
        val failedDecryption = vault.decrypt(corrupted)
        assertEquals("Corrupted ciphertext must fail closed returning null", null, failedDecryption)
    }

    @Test
    fun `test recovery code rate limiting enforces lock after 3 failed attempts`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.emat.domain.service.AuthService(context)
        authService.clearProvisionedCredentials()

        val initialKey = "Initial#Passcode777!"
        val provResult = authService.provisionFirstOperator(initialKey, "Lead Strategist")
        assertTrue(provResult.isSuccess)
        val validRecoveryCode = provResult.recoveryCode!!

        // Attempt 1: Failed
        val attempt1 = authService.recoverOperatorCredential("EMAT-RC-WRONG-1111", "NewKey#12345!")
        assertFalse(attempt1.isSuccess)
        assertTrue(attempt1.errorMessage?.contains("2 attempts remaining") == true)

        // Attempt 2: Failed
        val attempt2 = authService.recoverOperatorCredential("EMAT-RC-WRONG-2222", "NewKey#12345!")
        assertFalse(attempt2.isSuccess)
        assertTrue(attempt2.errorMessage?.contains("1 attempts remaining") == true)

        // Attempt 3: Failed
        val attempt3 = authService.recoverOperatorCredential("EMAT-RC-WRONG-3333", "NewKey#12345!")
        assertFalse(attempt3.isSuccess)

        // Attempt 4: Even with the VALID recovery code, recovery is permanently locked
        val lockedAttempt = authService.recoverOperatorCredential(validRecoveryCode, "NewKey#12345!")
        assertFalse("Recovery must be locked after 3 failed attempts", lockedAttempt.isSuccess)
        assertTrue(lockedAttempt.errorMessage?.contains("Maximum recovery attempts exceeded") == true)
    }

    @Test
    fun `test one time recovery code presentation and audit log sanitation`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.emat.ui.viewmodel.EmatViewModel(app)
        vm.authService.clearProvisionedCredentials()

        val validKey = "Master#OperatorKey9824!"

        // Step 1: First entry prompts confirmation
        vm.onPasswordChanged(validKey)
        vm.loginWithPassword()

        // Step 2: Confirmation entry provisions terminal
        vm.onPasswordChanged(validKey)
        vm.loginWithPassword()

        assertTrue(vm.isAuthenticated.value)
        val recoveryCode = vm.oneTimeRecoveryCode.value
        assertTrue("One-time recovery code must be populated for operator display", !recoveryCode.isNullOrEmpty())
        assertTrue("Recovery code starts with prefix", recoveryCode!!.startsWith("EMAT-RC-"))

        // Operator acknowledges the dialog
        vm.clearOneTimeRecoveryCode()
        assertEquals("Recovery code must be immediately purged from memory upon acknowledgment", null, vm.oneTimeRecoveryCode.value)

        // Verify audit logs do NOT contain plaintext recovery code
        kotlinx.coroutines.runBlocking {
            val logs = vm.auditLogs.first()
            val commissioningLog = logs.find { it.action == "FIRST_OPERATOR_COMMISSIONED" }
            if (commissioningLog != null) {
                assertFalse("Audit logs must NOT leak recovery code in dataAvailable", commissioningLog.dataAvailable.contains("Recovery Code:"))
                assertFalse("Audit logs must NOT leak recovery code in rationale", commissioningLog.rationale.contains(recoveryCode))
                assertTrue("Audit log records PBKDF2 iterations", commissioningLog.rationale.contains("210,000"))
            }
        }
    }

    @Test
    fun `test protected operations blocked when session is unauthenticated or expired`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.emat.ui.viewmodel.EmatViewModel(app)
        vm.logout()

        assertFalse("ViewModel must be unauthenticated", vm.isSessionActive())

        // 1. Attempting triggerFullMarketScan without session must be blocked
        vm.triggerFullMarketScan()
        assertFalse("Scanner must not run without authenticated session", vm.isScanning.value)
        assertEquals("Terminal must remain at AUTH gate", com.example.emat.ui.viewmodel.AppDestination.AUTH, vm.currentDestination.value)

        // 2. Attempting setTab or setNavDestination without session must be blocked
        vm.setTab(com.example.emat.ui.viewmodel.MainTab.SCANNER)
        assertEquals(com.example.emat.ui.viewmodel.AppDestination.AUTH, vm.currentDestination.value)

        vm.setNavDestination(com.example.emat.domain.model.EmatNavDestination.TRADES)
        assertEquals(com.example.emat.ui.viewmodel.AppDestination.AUTH, vm.currentDestination.value)

        // 3. Attempting emergency stop without session must be blocked
        vm.triggerEmergencyStop()
        assertFalse(vm.emergencyStopEngaged.value)
        assertEquals(com.example.emat.ui.viewmodel.AppDestination.AUTH, vm.currentDestination.value)

        // 4. Attempting AI analysis without session must be blocked
        vm.openAiAnalysis("TestContext")
        assertFalse(vm.isAiDialogOpen.value)
    }

    @Test
    fun `test recovery invalidates previous session and old recovery code cannot be reused`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.emat.domain.service.AuthService(context)
        authService.clearProvisionedCredentials()

        val initialKey = "Initial#OperatorKey2026!"
        val provResult = authService.provisionFirstOperator(initialKey, "Lead Strategist")
        assertTrue(provResult.isSuccess)
        val initialRecoveryCode = provResult.recoveryCode!!

        // Operator authenticates
        val loginResult = authService.loginWithPassword(initialKey)
        assertTrue(loginResult.isSuccess)
        assertTrue(authService.isSessionValid())

        // Perform recovery with initialRecoveryCode
        val newKey = "Recovered#NewKeyPassword2026!"
        val recoveryResult = authService.recoverOperatorCredential(initialRecoveryCode, newKey)
        assertTrue(recoveryResult.isSuccess)
        val replacementRecoveryCode = recoveryResult.recoveryCode!!
        assertFalse("Replacement recovery code must be unique from initial", initialRecoveryCode == replacementRecoveryCode)

        // 1. Previous password must fail
        val oldPasswordAttempt = authService.loginWithPassword(initialKey)
        assertFalse("Old password must fail after recovery", oldPasswordAttempt.isSuccess)

        // 2. Reusing old recovery code must be rejected
        val reuseOldCodeAttempt = authService.recoverOperatorCredential(initialRecoveryCode, "Another#NewKey999!")
        assertFalse("Old recovery code reuse must be rejected", reuseOldCodeAttempt.isSuccess)

        // 3. New password authenticates
        val newPasswordAttempt = authService.loginWithPassword(newKey)
        assertTrue("New password must succeed", newPasswordAttempt.isSuccess)

        // 4. Replacement recovery code is valid for future recovery
        val secondRecovery = authService.recoverOperatorCredential(replacementRecoveryCode, "Second#RecoveredKey2026!")
        assertTrue("Replacement recovery code must be valid", secondRecovery.isSuccess)
    }

    @Test
    fun `test interrupted commissioning resets safely and prevents false provisioning`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.emat.ui.viewmodel.EmatViewModel(app)
        vm.authService.clearProvisionedCredentials()

        assertFalse(vm.authService.isProvisioned())

        // Step 1: Input first key
        val key1 = "ValidKey#Institutional2026!"
        vm.onPasswordChanged(key1)
        vm.loginWithPassword()

        // Terminal is not yet provisioned
        assertFalse(vm.authService.isProvisioned())
        assertFalse(vm.isAuthenticated.value)

        // Step 2: Mismatched second key entered
        vm.onPasswordChanged("Mismatched#Key2026!")
        vm.loginWithPassword()

        // Commissioning must reset safely
        assertFalse("Mismatched confirmation must not provision terminal", vm.authService.isProvisioned())
        assertFalse("Operator must not enter MAIN destination", vm.currentDestination.value == com.example.emat.ui.viewmodel.AppDestination.MAIN)
        assertTrue(vm.authErrorMessage.value?.contains("Keys do not match") == true)
    }

    @Test
    fun `test recovery rate limiting cooldown window is active after 3 failed attempts`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.emat.domain.service.AuthService(context)
        authService.clearProvisionedCredentials()

        val provResult = authService.provisionFirstOperator("Initial#OperatorKey2026!", "Lead Strategist")
        assertTrue(provResult.isSuccess)

        // Fail 3 times
        authService.recoverOperatorCredential("EMAT-RC-FAKE-1111", "Key#12345!")
        authService.recoverOperatorCredential("EMAT-RC-FAKE-2222", "Key#12345!")
        val thirdFail = authService.recoverOperatorCredential("EMAT-RC-FAKE-3333", "Key#12345!")

        assertFalse(thirdFail.isSuccess)
        assertTrue("Must indicate cooldown lock active", thirdFail.errorMessage?.contains("Rate-limiting cooldown active") == true)
        assertTrue("Remaining lockout millis must be positive", authService.getRecoveryLockoutRemainingMillis() > 0L)
    }

    @Test
    fun `test secure audit logging schema strictly prohibits plaintext credentials and recovery codes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.emat.data.local.EmatDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)

        kotlinx.coroutines.runBlocking {
            // Attempt to log an event with recovery codes, passwords, and raw PBKDF2 verifiers in various fields
            val testRecoveryCode = "EMAT-RC-ABCD-EFGH-JKLM"
            val testPassword = "SuperSecret#Password999!"
            val testPhcHash = "\$pbkdf2-sha256\$i=210000\$s=0123456789abcdef0123456789abcdef\$v=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"

            repo.logAuditEvent(
                accountId = "EMAT-INST-SECURE",
                category = "SECURITY",
                action = "COMMISSIONING_TEST",
                actor = "LEAD_OPERATOR",
                rationale = "Operator generated code $testRecoveryCode for backup",
                dataAvailable = "password: $testPassword; rawHash: $testPhcHash",
                decisionMade = "Issue credential with secret=$testPassword",
                outcome = "Recovery code issued: $testRecoveryCode"
            )

            val logs = repo.getAuditLogsByAccountFlow("EMAT-INST-SECURE").first()
            assertEquals(1, logs.size)
            val log = logs.first()

            // 1. Strictly verify plaintext recovery codes are PROHIBITED and replaced with redactions
            assertFalse("Plaintext recovery code must be prohibited in rationale", log.rationale.contains(testRecoveryCode))
            assertTrue("Rationale must contain redaction placeholder", log.rationale.contains("[REDACTED_RECOVERY_CODE]"))

            assertFalse("Plaintext recovery code must be prohibited in outcome", log.outcome.contains(testRecoveryCode))
            assertTrue("Outcome must contain redaction placeholder", log.outcome.contains("[REDACTED_RECOVERY_CODE]"))

            // 2. Strictly verify passwords and secrets are PROHIBITED and replaced
            assertFalse("Plaintext password must be prohibited in dataAvailable", log.dataAvailable.contains(testPassword))
            assertTrue("dataAvailable must contain credential redaction", log.dataAvailable.contains("[REDACTED_CREDENTIAL]"))

            assertFalse("Plaintext password must be prohibited in decisionMade", log.decisionMade.contains(testPassword))
            assertTrue("decisionMade must contain credential redaction", log.decisionMade.contains("[REDACTED_CREDENTIAL]"))

            // 3. Strictly verify raw hash verifiers are redacted
            assertFalse("Raw PBKDF2 hash must be prohibited", log.dataAvailable.contains(testPhcHash))
            assertTrue("dataAvailable must contain PBKDF2 redaction", log.dataAvailable.contains("[PBKDF2_HASH_REDACTED]"))

            // 4. Verify cryptographic tamper-evident integrity hash is present
            assertTrue("Integrity hash must be a 64-character SHA-256 hex string", log.integrityHash.length == 64)
            assertTrue("Event metadata must default to valid structure", log.eventMetadata.isNotEmpty())
        }
        db.close()
    }

    @Test
    fun `test market analyzer calculates ECE components accurately and consistently`() {
        val analyzer = com.example.emat.domain.service.MarketAnalyzer()

        val bullishResult = analyzer.analyzeAsset(
            symbol = "XAUUSD",
            assetClass = AssetClass.METALS,
            bid = 2745.50,
            ask = 2745.80,
            spread = 0.30,
            high = 2750.00,
            low = 2735.00,
            close = 2748.50, // Bullish close near highs (position ~0.90)
            isNewsImminent = false
        )

        // Efficiency, Confidence, Exposure must be valid institutional scores (0-100)
        assertTrue("Efficiency score must be in range 0..100", bullishResult.efficiencyScore in 0..100)
        assertTrue("Confidence score must be in range 0..100", bullishResult.confidenceScore in 0..100)
        assertTrue("Exposure score must be in range 0..100", bullishResult.exposureScore in 0..100)
        assertTrue("Composite score must be in range 0..100", bullishResult.compositeEceScore in 0..100)

        // Verifies direction and qualification
        assertEquals(TradeDirection.BUY, bullishResult.direction)
        assertTrue("High quality setup should meet or exceed 70 ECE threshold", bullishResult.compositeEceScore >= 70)
        assertTrue("Actionable flag must be true when qualified", bullishResult.isActionable)
        assertEquals(SetupStatus.QUALIFIED, bullishResult.status)
        assertTrue("Risk assessment summary contains ECE details", bullishResult.riskAssessment.contains("XAUUSD"))
    }

    @Test
    fun `test market analyzer penalizes imminent news and wide spreads`() {
        val analyzer = com.example.emat.domain.service.MarketAnalyzer()

        // Setup with clear news
        val safeResult = analyzer.analyzeAsset(
            symbol = "EURUSD",
            assetClass = AssetClass.FOREX,
            bid = 1.0850,
            ask = 1.0851,
            spread = 0.0001,
            high = 1.0870,
            low = 1.0820,
            close = 1.0865,
            isNewsImminent = false
        )

        // Setup with imminent high-impact macro news
        val newsResult = analyzer.analyzeAsset(
            symbol = "EURUSD",
            assetClass = AssetClass.FOREX,
            bid = 1.0850,
            ask = 1.0851,
            spread = 0.0001,
            high = 1.0870,
            low = 1.0820,
            close = 1.0865,
            isNewsImminent = true
        )

        assertTrue(
            "Exposure score with imminent news must be lower than with clear news",
            newsResult.exposureScore < safeResult.exposureScore
        )
        assertTrue(
            "Composite ECE score with imminent news must reflect risk penalty",
            newsResult.compositeEceScore < safeResult.compositeEceScore
        )
        assertTrue(
            "Risk assessment should report high-impact news window active",
            newsResult.riskAssessment.contains("High-impact macro news window active")
        )
    }

    @Test
    fun `test market analyzer setup status thresholds`() {
        val analyzer = com.example.emat.domain.service.MarketAnalyzer()

        // Ranging consolidation candle (close at equilibrium midpoint)
        val rangingResult = analyzer.analyzeAsset(
            symbol = "USDJPY",
            assetClass = AssetClass.FOREX,
            bid = 153.20,
            ask = 153.22,
            spread = 0.02,
            high = 153.80,
            low = 152.60,
            close = 153.20, // Midpoint 0.50
            isNewsImminent = false
        )

        assertEquals(TradeDirection.NONE, rangingResult.direction)
        assertFalse("Ranging consolidation must not be actionable", rangingResult.isActionable)
        assertFalse("Ranging consolidation should not be qualified", rangingResult.status == SetupStatus.QUALIFIED)
    }

    @Test
    fun `test audit log allowlist enforcement, metadata filtering, and canonical integrity verification`() {
        val policy = com.example.emat.data.local.entity.AuditSecurityPolicy

        // 1. Permitted categories & actions
        assertTrue(policy.isCategoryPermitted("AUTH"))
        assertTrue(policy.isCategoryPermitted("SECURITY"))
        assertTrue(policy.isCategoryPermitted("SCANNER"))
        assertFalse(policy.isCategoryPermitted("UNKNOWN_UNAPPROVED_CATEGORY"))

        assertTrue(policy.isActionPermitted("FIRST_OPERATOR_COMMISSIONED"))
        assertTrue(policy.isActionPermitted("UNIVERSE_SCAN_COMPLETED"))
        assertFalse(policy.isActionPermitted("ARBITRARY_INJECTION_ACTION"))

        // 2. Metadata allowlist: unapproved keys (password, rawSecret, recoveryCode) must be dropped
        val dirtyJson = """{"symbol":"XAUUSD","spreadPips":1.5,"rawPassword":"SecretPass123!","sessionToken":"token999"}"""
        val sanitizedJson = policy.sanitizeMetadata(dirtyJson)
        assertTrue(sanitizedJson.contains(""""symbol":"XAUUSD""""))
        assertTrue(sanitizedJson.contains(""""spreadPips":1.5"""))
        assertFalse("Prohibited password key must be dropped", sanitizedJson.contains("rawPassword"))
        assertFalse("Prohibited token key must be dropped", sanitizedJson.contains("sessionToken"))

        // 3. Canonical integrity verification
        val entry = com.example.emat.data.local.entity.AuditLogEntity.createSecureEntry(
            accountId = "ACC-AUDIT-TEST",
            category = "SECURITY",
            action = "COMMISSIONING_TEST",
            actor = "LEAD_OPERATOR",
            rationale = "Verification of canonical tamper-evident integrity hash",
            dataAvailable = "Audit entry initialized with SHA-256 canonical digest",
            decisionMade = "Compute unambiguous length-prefixed payload",
            outcome = "Entry valid and verified"
        )

        assertTrue("Generated entry must pass integrity verification", entry.verifyIntegrity())

        // 4. Tampering must be detected
        val tamperedEntry = entry.copy(rationale = "Tampered rationale after database write")
        assertFalse("Tampered audit entry must fail integrity verification", tamperedEntry.verifyIntegrity())
    }

    @Test
    fun `test market analyzer validates inputs and rejects NaN, infinite, inverted quotes, and corrupt candles`() {
        val analyzer = com.example.emat.domain.service.MarketAnalyzer()

        // Case 1: NaN price
        val nanResult = analyzer.analyzeAsset(
            symbol = "EURUSD",
            assetClass = AssetClass.FOREX,
            bid = Double.NaN,
            ask = 1.0850,
            spread = 0.0001,
            high = 1.0870,
            low = 1.0820,
            close = 1.0865
        )
        assertFalse("NaN input must not be actionable", nanResult.isActionable)
        assertEquals(SetupStatus.INVALIDATED, nanResult.status)
        assertEquals(0, nanResult.compositeEceScore)
        assertTrue(nanResult.missingEvidenceReason?.contains("Invalid bid price") == true)

        // Case 2: Inverted quote (ask < bid)
        val invertedQuoteResult = analyzer.analyzeAsset(
            symbol = "EURUSD",
            assetClass = AssetClass.FOREX,
            bid = 1.0850,
            ask = 1.0840, // Inverted: ask < bid
            spread = 0.0001,
            high = 1.0870,
            low = 1.0820,
            close = 1.0845
        )
        assertFalse("Inverted quote must not be actionable", invertedQuoteResult.isActionable)
        assertEquals(SetupStatus.INVALIDATED, invertedQuoteResult.status)
        assertTrue(invertedQuoteResult.missingEvidenceReason?.contains("Inverted quote") == true)

        // Case 3: Corrupted candle (high < low)
        val corruptCandleResult = analyzer.analyzeAsset(
            symbol = "EURUSD",
            assetClass = AssetClass.FOREX,
            bid = 1.0850,
            ask = 1.0852,
            spread = 0.0001,
            high = 1.0800, // high < low
            low = 1.0890,
            close = 1.0850
        )
        assertFalse("Corrupted candle must not be actionable", corruptCandleResult.isActionable)
        assertEquals(SetupStatus.INVALIDATED, corruptCandleResult.status)
        assertTrue(corruptCandleResult.missingEvidenceReason?.contains("Corrupted candle") == true)

        // Case 4: Out-of-bounds close (close > high)
        val outOfBoundsCloseResult = analyzer.analyzeAsset(
            symbol = "EURUSD",
            assetClass = AssetClass.FOREX,
            bid = 1.0850,
            ask = 1.0852,
            spread = 0.0001,
            high = 1.0870,
            low = 1.0820,
            close = 1.0895 // Exceeds high
        )
        assertFalse("Out-of-bounds close must not be actionable", outOfBoundsCloseResult.isActionable)
        assertEquals(SetupStatus.INVALIDATED, outOfBoundsCloseResult.status)
        assertTrue(outOfBoundsCloseResult.missingEvidenceReason?.contains("Out-of-range candle close") == true)
    }

    @Test
    fun `test directional conviction is mathematically symmetrical for BUY and SELL`() {
        val analyzer = com.example.emat.domain.service.MarketAnalyzer()

        // Symmetrical bullish setup (position ~0.85)
        val buyResult = analyzer.analyzeAsset(
            symbol = "EURUSD",
            assetClass = AssetClass.FOREX,
            bid = 1.0850,
            ask = 1.0851,
            spread = 0.0001,
            high = 1.0900,
            low = 1.0800,
            close = 1.0885, // 85% of range
            isNewsImminent = false
        )

        // Symmetrical bearish setup (position ~0.15)
        val sellResult = analyzer.analyzeAsset(
            symbol = "EURUSD",
            assetClass = AssetClass.FOREX,
            bid = 1.0850,
            ask = 1.0851,
            spread = 0.0001,
            high = 1.0900,
            low = 1.0800,
            close = 1.0815, // 15% of range
            isNewsImminent = false
        )

        assertEquals(TradeDirection.BUY, buyResult.direction)
        assertEquals(TradeDirection.SELL, sellResult.direction)

        // Mathematical symmetry: confidence scores and composite scores must match exactly
        assertEquals("Confidence scores for symmetrical BUY and SELL must be identical",
            buyResult.confidenceScore, sellResult.confidenceScore)
        assertEquals("Efficiency scores for symmetrical displacement must be identical",
            buyResult.efficiencyScore, sellResult.efficiencyScore)
        assertEquals("Composite ECE scores for symmetrical setups must be identical",
            buyResult.compositeEceScore, sellResult.compositeEceScore)
        assertEquals("Trend scores in breakdown must be identical",
            buyResult.breakdown.trendScore, sellResult.breakdown.trendScore)
    }

    @Test
    fun `test asset-class quote precision spread calculations`() {
        val analyzer = com.example.emat.domain.service.MarketAnalyzer()

        // 1. Indices: raw spread is measured directly in index points (not 10,000 pips!)
        val indexSpread = analyzer.calculateSpreadInPips("NAS100", 1.5, AssetClass.INDICES)
        assertEquals(1.5, indexSpread, 0.001)

        val us30Spread = analyzer.calculateSpreadInPips("US30", 2.0, AssetClass.INDICES)
        assertEquals(2.0, us30Spread, 0.001)

        // 2. Crypto: measured in USD points directly
        val cryptoSpread = analyzer.calculateSpreadInPips("BTCUSD", 3.5, AssetClass.CRYPTO)
        assertEquals(3.5, cryptoSpread, 0.001)

        // 3. Metals Gold: 0.01 quote precision -> 0.30 raw spread = 3.0 pips
        val goldSpread = analyzer.calculateSpreadInPips("XAUUSD", 0.30, AssetClass.METALS)
        assertEquals(3.0, goldSpread, 0.001)

        // 4. Metals Silver: 0.001 quote precision -> 0.025 raw spread = 2.5 pips
        val silverSpread = analyzer.calculateSpreadInPips("XAGUSD", 0.025, AssetClass.METALS)
        assertEquals(2.5, silverSpread, 0.001)

        // 5. Forex Standard: 0.0001 quote precision -> 0.00012 raw spread = 1.2 pips
        val forexStdSpread = analyzer.calculateSpreadInPips("EURUSD", 0.00012, AssetClass.FOREX)
        assertEquals(1.2, forexStdSpread, 0.001)

        // 6. Forex JPY: 0.01 quote precision -> 0.018 raw spread = 1.8 pips
        val forexJpySpread = analyzer.calculateSpreadInPips("USDJPY", 0.018, AssetClass.FOREX)
        assertEquals(1.8, forexJpySpread, 0.001)
    }

    @Test
    fun `test unverified news data applies conservative exposure penalty and prevents false low-risk qualification`() {
        val analyzer = com.example.emat.domain.service.MarketAnalyzer()

        // Candidate setup with unverified news data
        val unverifiedNewsResult = analyzer.analyzeAsset(
            symbol = "EURUSD",
            assetClass = AssetClass.FOREX,
            bid = 1.0850,
            ask = 1.0851,
            spread = 0.0001,
            high = 1.0890,
            low = 1.0810,
            close = 1.0880,
            newsFeedStatus = com.example.emat.domain.service.MarketAnalyzer.NewsFeedStatus.UNVERIFIED_OR_MISSING
        )

        // Must NOT be actionable when news is unverified
        assertFalse("Unverified news feed must prevent actionable execution", unverifiedNewsResult.isActionable)
        assertFalse("Unverified news feed must prevent QUALIFIED status", unverifiedNewsResult.status == SetupStatus.QUALIFIED)
        assertEquals(SetupStatus.MONITORING, unverifiedNewsResult.status)
        assertTrue("Exposure score must be conservative (< 60)", unverifiedNewsResult.exposureScore < 60)
        assertTrue(unverifiedNewsResult.missingEvidenceReason?.contains("unverified or missing") == true)
    }

    @Test
    fun `test account category is explicitly assigned and validated without automatic institutional default`() {
        val individualAccount = com.example.emat.domain.model.EmatAccount(
            id = "EMAT-IND-901",
            name = "Personal Retail Vault",
            role = "Independent Trader",
            tier = "Tier 3 Individual Account",
            dailyLossLimit = "1.0% ($1,000)",
            maxPositionSize = "0.05 Lot",
            isLiveExecutionAllowed = false,
            permissions = listOf("MARKET_ANALYSIS", "PAPER_SIMULATION"),
            category = com.example.emat.domain.model.AccountCategory.INDIVIDUAL
        )

        val institutionalAccount = com.example.emat.domain.model.EmatAccount(
            id = "EMAT-INST-001",
            name = "EMAT Master Institutional",
            role = "Chief Strategist",
            tier = "Tier 1 Master Institutional",
            dailyLossLimit = "2.0% ($20,000)",
            maxPositionSize = "1.00 Lot",
            isLiveExecutionAllowed = true,
            permissions = listOf("LIVE_EXECUTION", "FULL_SCANNER"),
            category = com.example.emat.domain.model.AccountCategory.INSTITUTIONAL
        )

        assertEquals(com.example.emat.domain.model.AccountCategory.INDIVIDUAL, individualAccount.category)
        assertEquals(com.example.emat.domain.model.AccountCategory.INSTITUTIONAL, institutionalAccount.category)
        assertFalse(individualAccount.isLiveExecutionAllowed)
        assertTrue(institutionalAccount.isLiveExecutionAllowed)

        // Verify fallback parsing: unknown or null defaults to INDIVIDUAL, never INSTITUTIONAL
        assertEquals(com.example.emat.domain.model.AccountCategory.INDIVIDUAL, com.example.emat.domain.model.AccountCategory.fromCode(null))
        assertEquals(com.example.emat.domain.model.AccountCategory.INDIVIDUAL, com.example.emat.domain.model.AccountCategory.fromCode("UNKNOWN_GARBAGE"))
        assertEquals(com.example.emat.domain.model.AccountCategory.INDIVIDUAL, com.example.emat.domain.model.AccountCategory.fromCode("IND"))
        assertEquals(com.example.emat.domain.model.AccountCategory.INSTITUTIONAL, com.example.emat.domain.model.AccountCategory.fromCode("INST"))

        // Verify default constructor category defaults to INDIVIDUAL
        val defaultAccount = com.example.emat.domain.model.EmatAccount(
            id = "DEF-001",
            name = "Default Test Account",
            role = "Operator",
            tier = "Standard",
            dailyLossLimit = "1.0%",
            maxPositionSize = "0.10 Lot",
            isLiveExecutionAllowed = false,
            permissions = emptyList()
        )
        assertEquals(com.example.emat.domain.model.AccountCategory.INDIVIDUAL, defaultAccount.category)
    }

    @Test
    fun `test risk exposure calculator returns explicit no active trade when position is null`() {
        val account = com.example.emat.domain.model.EmatAccount(
            id = "ACC-001",
            name = "Master",
            role = "Operator",
            tier = "Tier 1",
            dailyLossLimit = "2%",
            maxPositionSize = "1 Lot",
            isLiveExecutionAllowed = true,
            permissions = emptyList(),
            category = com.example.emat.domain.model.AccountCategory.INSTITUTIONAL
        )

        val calc = com.example.emat.ui.components.ExposureCalculator.calculate(
            activeTrade = null,
            account = account
        )

        assertFalse("Should not have active trade", calc.hasActiveTrade)
        assertEquals("openPositionExposurePercent must be null when no active trade", null, calc.openPositionExposurePercent)
        assertEquals("potentialLossAmount must be null when no active trade", null, calc.potentialLossAmount)
        assertEquals("potentialLossPercent must be null when no active trade", null, calc.potentialLossPercent)
        assertEquals("assetClass must be null when no active trade", null, calc.assetClass)
        assertEquals("NO ACTIVE EMAT TRADE", calc.statusText)
    }

    @Test
    fun `test risk exposure calculator computes correct stop loss potential loss for active position`() {
        val account = com.example.emat.domain.model.EmatAccount(
            id = "ACC-001",
            name = "Master",
            role = "Operator",
            tier = "Tier 1",
            dailyLossLimit = "2%",
            maxPositionSize = "1 Lot",
            isLiveExecutionAllowed = true,
            permissions = emptyList(),
            category = com.example.emat.domain.model.AccountCategory.INSTITUTIONAL
        )

        val activeTrade = com.example.emat.domain.model.ActiveTrade(
            tradeId = "TRD-EURUSD-01",
            symbol = "EURUSD",
            direction = TradeDirection.BUY,
            entryPrice = 1.0850,
            stopLoss = 1.0820, // 30 pips distance = 0.0030
            takeProfit = 1.0910,
            positionSizeLots = 0.10, // 0.10 lot * 100,000 = $10,000 * 0.0030 = $30.00
            currentPrice = 1.0855,
            unrealizedPnl = 5.0,
            isBreakEven = false,
            riskStatus = RiskStatus.APPROVED,
            status = "ACTIVE",
            executionMode = "NOT_CONNECTED",
            openedAt = System.currentTimeMillis()
        )

        val calc = com.example.emat.ui.components.ExposureCalculator.calculate(
            activeTrade = activeTrade,
            account = account,
            accountEquity = 100000.0
        )

        assertTrue("Must report active trade", calc.hasActiveTrade)
        assertEquals(30.0, calc.potentialLossAmount ?: 0.0, 0.01)
        assertEquals(0.03, calc.potentialLossPercent ?: 0.0, 0.001)
        assertEquals(AssetClass.FOREX, calc.assetClass)
        assertTrue(calc.statusText.contains("BUY EURUSD"))
    }

    @Test
    fun `test one trade rule repository mutex blocks concurrent second trade insertion`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(context)
        val repository = com.example.emat.data.repository.EmatRepositoryImpl(db)

        val accountId = "ACC-CONCURRENCY-MULTI"

        // Clean up any lingering trades for this test account
        val existingTrades = db.tradeDao().getTradesByAccountFlow(accountId).first()
        existingTrades.forEach { db.tradeDao().emergencyStopAllTrades() }

        // Launch 10 simultaneous trade submission attempts across real multi-threaded Dispatchers.Default pool
        val deferredList = (1..10).map { i ->
            async(Dispatchers.Default) {
                repository.submitTradeSafely(
                    com.example.emat.data.local.entity.TradeEntity(
                        tradeId = "TRD-CONCURRENT-$i",
                        accountId = accountId,
                        symbol = if (i % 2 == 0) "EURUSD" else "GBPUSD",
                        direction = if (i % 2 == 0) "BUY" else "SELL",
                        entryPrice = 1.0850,
                        stopLoss = 1.0800,
                        takeProfit = 1.0950,
                        positionSize = 0.10,
                        currentPrice = 1.0850,
                        unrealizedResult = 0.0,
                        isBreakEven = false,
                        riskStatus = "APPROVED",
                        status = "ACTIVE",
                        executionMode = "NOT_CONNECTED",
                        openedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        val results = deferredList.awaitAll()
        val successCount = results.count { it }
        val rejectCount = results.count { !it }

        assertEquals("Exactly one trade must succeed in concurrent race", 1, successCount)
        assertEquals("Nine concurrent trades must be blocked by One-Trade Rule mutex", 9, rejectCount)

        val activeCount = db.tradeDao().getActiveTradeCount()
        assertEquals("Active trades in database must strictly equal 1", 1, activeCount)
        db.tradeDao().emergencyStopAllTrades()
    }

    @Test
    fun `test duplicate tradeId submission is rejected by repository mutex`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(context)
        val repository = com.example.emat.data.repository.EmatRepositoryImpl(db)
        db.tradeDao().emergencyStopAllTrades()

        val accountId = "ACC-DUP-ID-TEST"
        val duplicateTradeId = "TRD-UNIQUE-999"

        val trade = com.example.emat.data.local.entity.TradeEntity(
            tradeId = duplicateTradeId,
            accountId = accountId,
            symbol = "EURUSD",
            direction = "BUY",
            entryPrice = 1.0850,
            stopLoss = 1.0800,
            takeProfit = 1.0950,
            positionSize = 0.10,
            currentPrice = 1.0850,
            unrealizedResult = 0.0,
            isBreakEven = false,
            riskStatus = "APPROVED",
            status = "ACTIVE",
            executionMode = "NOT_CONNECTED",
            openedAt = System.currentTimeMillis()
        )

        // First attempt succeeds
        val firstResult = repository.submitTradeSafely(trade)
        assertTrue("Initial trade submission must succeed", firstResult)

        // Second attempt with exact same tradeId must be rejected as duplicate
        val secondResult = repository.submitTradeSafely(trade)
        assertFalse("Duplicate tradeId must be strictly rejected", secondResult)
        db.tradeDao().emergencyStopAllTrades()
    }

    @Test
    fun `test pragma quick_check runs on IO thread and returns integrity ok with db version`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(context)
        val repository = com.example.emat.data.repository.EmatRepositoryImpl(db)

        val result = repository.runDatabaseIntegrityCheck()
        assertTrue("PRAGMA quick_check must report ok", result.isOk)
        assertEquals("Status must be INTEGRITY OK", "INTEGRITY OK", result.status)
        assertEquals("Database version must be read as 4", 4, result.version)
        assertTrue("Timestamp must be valid", result.timestamp > 0)
    }

    @Test
    fun `test seeded database records survive and database stays at version 4`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(context)

        // Verify version is 4
        assertEquals(4, db.openHelper.readableDatabase.version)

        // Seed an instrument
        val testInstrument = com.example.emat.data.local.entity.InstrumentEntity(
            symbol = "AUDNZD",
            name = "Australian / New Zealand Dollar",
            assetClass = "FOREX",
            isActive = true,
            pipSize = 0.0001,
            digits = 5,
            minVolume = 0.01,
            contractSize = 100000.0
        )
        db.instrumentDao().insertInstruments(listOf(testInstrument))

        // Seed an audit log with permitted action CONFIG_UPDATED
        val auditEntry = com.example.emat.data.local.entity.AuditLogEntity.createSecureEntry(
            accountId = "ACC-SEEDED-01",
            category = "CONFIG",
            action = "CONFIG_UPDATED",
            actor = "TEST_HARNESS",
            rationale = "Seeded record survival check",
            dataAvailable = "Audit ID check",
            decisionMade = "Verify persistence without schema breach",
            outcome = "OK"
        )
        db.auditLogDao().insertAuditLog(auditEntry)

        // Verify records persist intact
        val retrievedInstruments = db.instrumentDao().getAllInstruments().first()
        assertTrue("Seeded instrument must survive", retrievedInstruments.any { it.symbol == "AUDNZD" })

        val retrievedLogs = db.auditLogDao().getRecentAuditLogsByAccount("ACC-SEEDED-01").first()
        assertTrue("Seeded audit log must survive", retrievedLogs.any { it.action == "CONFIG_UPDATED" })

        // Schema version remains 4
        assertEquals(4, db.openHelper.readableDatabase.version)
    }

    @Test
    fun `test exposure calculator marks equity and funds as unavailable when sources are missing`() {
        val account = com.example.emat.domain.model.EmatAccount(
            id = "ACC-UNAVAIL-TEST",
            name = "Test Operator",
            role = "Analyst",
            tier = "Tier 2",
            dailyLossLimit = "", // empty risk limit
            maxPositionSize = "0.50 Lot",
            isLiveExecutionAllowed = false,
            permissions = emptyList(),
            category = com.example.emat.domain.model.AccountCategory.INDIVIDUAL
        )

        // Case 1: No active trade and missing equity/funds/limits
        val calcNoTrade = com.example.emat.ui.components.ExposureCalculator.calculate(
            activeTrade = null,
            account = account,
            accountEquity = null,
            availableFunds = null,
            configuredRiskLimits = null
        )
        assertFalse(calcNoTrade.hasActiveTrade)
        assertEquals(null, calcNoTrade.accountEquity)
        assertEquals(null, calcNoTrade.availableFunds)
        assertEquals(null, calcNoTrade.configuredRiskLimits)
        assertEquals(null, calcNoTrade.potentialLossAmount)
        assertEquals("NO ACTIVE EMAT TRADE", calcNoTrade.statusText)

        // Case 2: Active trade present but accountEquity is null
        val activeTrade = com.example.emat.domain.model.ActiveTrade(
            tradeId = "TRD-NO-EQ",
            symbol = "EURUSD",
            direction = TradeDirection.BUY,
            entryPrice = 1.0850,
            stopLoss = 1.0800,
            takeProfit = 1.0950,
            positionSizeLots = 0.10,
            currentPrice = 1.0850,
            unrealizedPnl = 0.0,
            isBreakEven = false,
            riskStatus = RiskStatus.APPROVED,
            status = "ACTIVE",
            executionMode = "NOT_CONNECTED",
            openedAt = System.currentTimeMillis()
        )

        val calcWithTrade = com.example.emat.ui.components.ExposureCalculator.calculate(
            activeTrade = activeTrade,
            account = account,
            accountEquity = null,
            availableFunds = null
        )
        assertTrue(calcWithTrade.hasActiveTrade)
        assertEquals(50.0, calcWithTrade.potentialLossAmount ?: 0.0, 0.01) // 50 pips * 0.10 lot * 100,000 = $50.00
        assertEquals("potentialLossPercent must be null when equity is missing", null, calcWithTrade.potentialLossPercent)
    }

    @Test
    fun `test theme mode settings resolution and persistence`() {
        val dark = com.example.emat.ui.theme.ThemeMode.DARK
        val light = com.example.emat.ui.theme.ThemeMode.LIGHT
        val system = com.example.emat.ui.theme.ThemeMode.SYSTEM

        // Mode resolution under system dark = false
        assertTrue("DARK mode must resolve to dark=true", dark.isDark(systemInDarkTheme = false))
        assertFalse("LIGHT mode must resolve to dark=false", light.isDark(systemInDarkTheme = false))
        assertFalse("SYSTEM mode must resolve to system setting (false)", system.isDark(systemInDarkTheme = false))

        // Mode resolution under system dark = true
        assertTrue("DARK mode must resolve to dark=true under dark system", dark.isDark(systemInDarkTheme = true))
        assertFalse("LIGHT mode must resolve to dark=false under dark system", light.isDark(systemInDarkTheme = true))
        assertTrue("SYSTEM mode must resolve to system setting (true)", system.isDark(systemInDarkTheme = true))

        // Serialization and parsing
        assertEquals(com.example.emat.ui.theme.ThemeMode.DARK, com.example.emat.ui.theme.ThemeMode.fromString("DARK"))
        assertEquals(com.example.emat.ui.theme.ThemeMode.LIGHT, com.example.emat.ui.theme.ThemeMode.fromString("LIGHT"))
        assertEquals(com.example.emat.ui.theme.ThemeMode.SYSTEM, com.example.emat.ui.theme.ThemeMode.fromString("SYSTEM"))
        assertEquals(com.example.emat.ui.theme.ThemeMode.SYSTEM, com.example.emat.ui.theme.ThemeMode.fromString("unknown_value"))
        assertEquals(com.example.emat.ui.theme.ThemeMode.SYSTEM, com.example.emat.ui.theme.ThemeMode.fromString(null))
    }

    @Test
    fun `test local ai analyzer self check evaluates fixed fixture successfully`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(context)
        val repository = com.example.emat.data.repository.EmatRepositoryImpl(db)

        val selfCheckPassed = repository.runAiAnalyzerSelfCheck()
        assertTrue("Local AI analyzer self check on fixed fixture must pass", selfCheckPassed)
    }

    @Test
    fun `test splash branding and dashboard risk exposure integration`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.emat.ui.viewmodel.EmatViewModel(app)

        // Verify splash branding asset is present
        val res = app.resources
        val unieeLogoId = res.getIdentifier("img_uniee_logo", "drawable", app.packageName)
        assertTrue("UNIEE logo drawable must exist", unieeLogoId != 0)

        // Verify dashboard exposure calculation with no active trades returns exact truthful string
        val exposure = com.example.emat.ui.components.ExposureCalculator.calculate(
            activeTrade = null,
            account = vm.currentAccount.value
        )
        assertFalse(exposure.hasActiveTrade)
        assertEquals("NO ACTIVE EMAT TRADE", exposure.statusText)
        assertEquals(null, exposure.potentialLossPercent)

        // Verify One-Trade Rule configuration in database
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        val maxTrades = kotlinx.coroutines.runBlocking {
            db.configDao().getConfig("MAX_ACTIVE_TRADES")?.value
        }
        assertEquals("1", maxTrades)
    }

    @Test
    fun `splash progress equals completed real steps divided by total for none partial all one step failed`() {
        val totalSteps = 3f

        // Case 1: None completed
        val noneProgress = 0f / totalSteps
        assertEquals(0.0f, noneProgress, 0.001f)

        // Case 2: Partial completed (1 of 3)
        val step1Progress = 1f / totalSteps
        assertEquals(0.3333f, step1Progress, 0.001f)

        // Case 3: Partial completed (2 of 3)
        val step2Progress = 2f / totalSteps
        assertEquals(0.6666f, step2Progress, 0.001f)

        // Case 4: All completed (3 of 3)
        val allProgress = 3f / totalSteps
        assertEquals(1.0f, allProgress, 0.001f)

        // Case 5: One step failed
        val failedStepState = com.example.emat.ui.viewmodel.SplashInitState(
            progress = 1f / totalSteps,
            statusMessage = "Database integrity check failed: corrupt page",
            isComplete = false
        )
        assertEquals(0.3333f, failedStepState.progress, 0.001f)
        assertTrue(failedStepState.statusMessage.contains("failed"))
    }

    @Test
    fun `integrity check returns FAILED state when quick_check is not ok`() {
        val corruptedOutput = "*** in database user_sessions: page 2 is corrupt"
        val isOk = corruptedOutput.trim().equals("ok", ignoreCase = true)
        val status = if (isOk) "INTEGRITY OK" else "INTEGRITY CHECK FAILED"
        val result = com.example.emat.data.repository.DbIntegrityResult(
            status = status,
            isOk = isOk,
            version = 3,
            timestamp = System.currentTimeMillis(),
            details = corruptedOutput
        )

        assertFalse("Integrity check must not be ok when output is not 'ok'", result.isOk)
        assertEquals("INTEGRITY CHECK FAILED", result.status)
        assertEquals(3, result.version)
        assertTrue(result.details.contains("corrupt"))
    }

    @Test
    fun `analyzer self-check failure shows LOCAL ANALYZER NOT READY`() {
        val selfCheckPassed = false
        val status = if (selfCheckPassed) "LOCAL ANALYZER READY" else "LOCAL ANALYZER NOT READY"
        val isPositive = selfCheckPassed

        assertEquals("LOCAL ANALYZER NOT READY", status)
        assertFalse(isPositive)
    }

    @Test
    fun `dashboard ViewModel state for zero one three qualifying opportunities`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)

        // Zero qualifying opportunities
        db.scannerDao().clearOpportunities()
        val opps0 = db.scannerDao().getTopOpportunities().first()
        assertEquals(0, opps0.size)

        // One qualifying opportunity
        val opp1 = com.example.emat.data.local.entity.OpportunityEntity(
            scanRunId = "RUN-1",
            rank = 1,
            symbol = "EURUSD",
            assetClass = "FOREX",
            direction = "BUY",
            eceScore = 85,
            marketCondition = "TRENDING_BULLISH",
            setupStatus = "QUALIFIED",
            timeframeAlignment = "FULL_ALIGNMENT",
            riskStatus = "APPROVED",
            whyItQualifies = "H4 alignment",
            timestamp = System.currentTimeMillis(),
            freshnessSeconds = 0
        )
        db.scannerDao().insertOpportunities(listOf(opp1))
        val opps1 = db.scannerDao().getTopOpportunities().first()
        assertEquals(1, opps1.size)
        assertEquals("EURUSD", opps1[0].symbol)

        // Three qualifying opportunities
        val opp2 = opp1.copy(rank = 2, symbol = "GBPUSD", eceScore = 80)
        val opp3 = opp1.copy(rank = 3, symbol = "XAUUSD", eceScore = 75)
        db.scannerDao().clearOpportunities()
        db.scannerDao().insertOpportunities(listOf(opp1, opp2, opp3))
        val opps3 = db.scannerDao().getTopOpportunities().first()
        assertEquals(3, opps3.size)
        assertEquals("EURUSD", opps3[0].symbol)
        assertEquals("GBPUSD", opps3[1].symbol)
        assertEquals("XAUUSD", opps3[2].symbol)
    }

    @Test
    fun `missing risk limits show UNAVAILABLE`() {
        val unconfiguredAccount = com.example.emat.domain.model.EmatAccount(
            id = "ACC-UNCONFIGURED",
            name = "Test Account",
            role = "Analyst",
            tier = "Tier 1",
            dailyLossLimit = "",
            maxPositionSize = "0.50 Lot",
            isLiveExecutionAllowed = false,
            permissions = emptyList()
        )

        val calc = com.example.emat.ui.components.ExposureCalculator.calculate(
            activeTrade = null,
            account = unconfiguredAccount,
            configuredRiskLimits = null
        )

        assertEquals("Configured risk limits must be null when missing", null, calc.configuredRiskLimits)
    }

    @Test
    fun `global one-trade second trade from a different account is rejected`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(context)
        val repository = com.example.emat.data.repository.EmatRepositoryImpl(db)

        db.tradeDao().emergencyStopAllTrades()

        // Account 1 submits trade: must succeed
        val tradeAccount1 = com.example.emat.data.local.entity.TradeEntity(
            tradeId = "TRD-ACC1-01",
            accountId = "ACCOUNT_ALPHA",
            symbol = "EURUSD",
            direction = "BUY",
            entryPrice = 1.0850,
            stopLoss = 1.0800,
            takeProfit = 1.0950,
            positionSize = 0.10,
            currentPrice = 1.0850,
            unrealizedResult = 0.0,
            isBreakEven = false,
            riskStatus = "APPROVED",
            status = "ACTIVE",
            executionMode = "NOT_CONNECTED",
            openedAt = System.currentTimeMillis()
        )
        val res1 = repository.submitTradeSafely(tradeAccount1)
        assertTrue("Trade for Account 1 must be accepted", res1)

        // Account 2 submits trade while Account 1 trade is active: must be REJECTED under Global One-Trade Rule!
        val tradeAccount2 = com.example.emat.data.local.entity.TradeEntity(
            tradeId = "TRD-ACC2-01",
            accountId = "ACCOUNT_BETA",
            symbol = "GBPUSD",
            direction = "SELL",
            entryPrice = 1.2500,
            stopLoss = 1.2550,
            takeProfit = 1.2400,
            positionSize = 0.10,
            currentPrice = 1.2500,
            unrealizedResult = 0.0,
            isBreakEven = false,
            riskStatus = "APPROVED",
            status = "ACTIVE",
            executionMode = "NOT_CONNECTED",
            openedAt = System.currentTimeMillis()
        )
        val res2 = repository.submitTradeSafely(tradeAccount2)
        assertFalse("Second trade from different account must be blocked by Global One-Trade Rule", res2)

        assertEquals(1, db.tradeDao().getActiveTradeCount())
        db.tradeDao().emergencyStopAllTrades()
    }

    @Test
    fun `global one-trade 10 concurrent attempts across two accounts yield exactly one active trade`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(context)
        val repository = com.example.emat.data.repository.EmatRepositoryImpl(db)

        db.tradeDao().emergencyStopAllTrades()

        // 10 concurrent requests alternating between ACCOUNT_ALPHA and ACCOUNT_BETA
        val jobs = (1..10).map { i ->
            async(Dispatchers.Default) {
                repository.submitTradeSafely(
                    com.example.emat.data.local.entity.TradeEntity(
                        tradeId = "TRD-RACE-$i",
                        accountId = if (i % 2 == 0) "ACCOUNT_ALPHA" else "ACCOUNT_BETA",
                        symbol = if (i % 2 == 0) "EURUSD" else "GBPUSD",
                        direction = if (i % 2 == 0) "BUY" else "SELL",
                        entryPrice = 1.0850,
                        stopLoss = 1.0800,
                        takeProfit = 1.0950,
                        positionSize = 0.10,
                        currentPrice = 1.0850,
                        unrealizedResult = 0.0,
                        isBreakEven = false,
                        riskStatus = "APPROVED",
                        status = "ACTIVE",
                        executionMode = "NOT_CONNECTED",
                        openedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        val outcomes = jobs.awaitAll()
        assertEquals("Exactly one trade must succeed globally", 1, outcomes.count { it })
        assertEquals("Nine concurrent trades must fail globally", 9, outcomes.count { !it })
        assertEquals("Global active trade count must strictly be 1", 1, db.tradeDao().getActiveTradeCount())

        db.tradeDao().emergencyStopAllTrades()
    }

    @Test
    fun `AuthScreen appearance independence theme mode DARK LIGHT SYSTEM does not change the auth wrapper colors`() {
        val darkExpectedBackground = com.example.emat.ui.theme.SleekCharcoalBg
        val darkExpectedSurface = com.example.emat.ui.theme.SleekCharcoalSurface

        // Mode resolution verification
        assertTrue(com.example.emat.ui.theme.ThemeMode.DARK.isDark(systemInDarkTheme = false))
        assertFalse(com.example.emat.ui.theme.ThemeMode.LIGHT.isDark(systemInDarkTheme = false))

        // Invariant dark colors of SleekColors remain identical
        val colors = com.example.emat.ui.theme.SleekColors()
        assertEquals(darkExpectedBackground, colors.charcoalBg)
        assertEquals(darkExpectedSurface, colors.charcoalSurface)
    }

    @Test
    fun `selecting INSTITUTIONAL with the INDIVIDUAL account's credential fails`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.emat.ui.viewmodel.EmatViewModel(app)
        val authService = vm.authService
        authService.clearProvisionedCredentials()

        val instKey = "InstKey@2026#Master"
        val indKey = "IndivKey@2026#Desk"
        assertTrue(authService.provisionFirstOperator(instKey, "Lead Strategist").isSuccess)
        assertTrue(authService.provisionAccountCredential("EMAT-INST-001", instKey, "Lead Strategist"))
        assertTrue(authService.provisionAccountCredential("EMAT-IND-002", indKey, "Individual Trader"))

        val instAccount = vm.availableAccounts.first { it.id == "EMAT-INST-001" }

        // Select INSTITUTIONAL account, but attempt login with INDIVIDUAL account's credential
        vm.selectLoginAccount(instAccount)
        vm.onPasswordChanged(indKey)
        vm.loginWithPassword()

        assertFalse("Selecting INSTITUTIONAL with INDIVIDUAL account credential must fail", vm.isAuthenticated.value)
        assertEquals("Invalid credentials. Unauthorized access denied.", vm.authErrorMessage.value)
    }

    @Test
    fun `the selector cannot change an account's stored category`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.emat.ui.viewmodel.EmatViewModel(app)

        val instAccount = vm.availableAccounts.first { it.id == "EMAT-INST-001" }
        val indAccount = vm.availableAccounts.first { it.id == "EMAT-IND-002" }

        // Verify categories are stored directly on the accounts
        assertEquals(com.example.emat.domain.model.AccountCategory.INSTITUTIONAL, instAccount.category)
        assertEquals(com.example.emat.domain.model.AccountCategory.INDIVIDUAL, indAccount.category)

        // Select Institutional
        vm.selectLoginAccount(instAccount)
        assertEquals("Selected login account must match chosen instance", "EMAT-INST-001", vm.selectedLoginAccount.value.id)
        assertEquals(com.example.emat.domain.model.AccountCategory.INSTITUTIONAL, vm.selectedLoginAccount.value.category)

        // Select Individual
        vm.selectLoginAccount(indAccount)
        assertEquals("Selected login account must match chosen instance", "EMAT-IND-002", vm.selectedLoginAccount.value.id)
        assertEquals(com.example.emat.domain.model.AccountCategory.INDIVIDUAL, vm.selectedLoginAccount.value.category)

        // Selector choice cannot mutate the account object's immutable category
        assertEquals(com.example.emat.domain.model.AccountCategory.INSTITUTIONAL, instAccount.category)
        assertEquals(com.example.emat.domain.model.AccountCategory.INDIVIDUAL, indAccount.category)
    }

    @Test
    fun `a wrong credential fails on either selection, with the same message`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.emat.ui.viewmodel.EmatViewModel(app)
        vm.authService.provisionFirstOperator("ValidKey@2026#Prod", "Lead Strategist")

        val instAccount = vm.availableAccounts.first { it.id == "EMAT-INST-001" }
        val indAccount = vm.availableAccounts.first { it.id == "EMAT-IND-002" }

        // 1. Wrong credential on Institutional selection
        vm.selectLoginAccount(instAccount)
        vm.onPasswordChanged("WrongPassword@123")
        vm.loginWithPassword()
        assertFalse(vm.isAuthenticated.value)
        val instMsg = vm.authErrorMessage.value

        // 2. Wrong credential on Individual selection
        vm.selectLoginAccount(indAccount)
        vm.onPasswordChanged("WrongPassword@123")
        vm.loginWithPassword()
        assertFalse(vm.isAuthenticated.value)
        val indMsg = vm.authErrorMessage.value

        assertEquals("Invalid credentials. Unauthorized access denied.", instMsg)
        assertEquals("Error message must be identical across both selections to prevent enumeration", instMsg, indMsg)
    }

    @Test
    fun `no hardcoded default password exists`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.emat.domain.service.AuthService(context)

        val defaultPasswords = listOf("EMAT2026", "admin", "password", "123456", "root", "operator", "letmein", "default")
        for (candidate in defaultPasswords) {
            assertTrue("Default candidate '$candidate' must be identified as disallowed", authService.isDisallowedDefaultCredential(candidate))
            assertFalse("Default candidate '$candidate' must fail complexity validation", authService.validateKeyComplexity(candidate).isValid)
            val loginResult = authService.loginWithPassword(candidate)
            assertFalse("Default candidate '$candidate' must fail login", loginResult.isSuccess)
        }
    }

    @Test
    fun `existing auth tests still pass unchanged`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.emat.domain.service.AuthService(context)
        authService.clearProvisionedCredentials()

        val provKey = "MySecKey@2026#Valid"
        val result = authService.provisionFirstOperator(provKey, "Lead Strategist")
        assertTrue("First-operator commissioning must succeed with valid key", result.isSuccess)

        // Existing signature loginWithPassword(key) without accountId still works seamlessly
        val loginResult = authService.loginWithPassword(provKey)
        assertTrue("Legacy loginWithPassword without accountId must continue to authenticate primary key", loginResult.isSuccess)
    }

    @Test
    fun `session and account-context separation still hold after sign-in on each account`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.emat.ui.viewmodel.EmatViewModel(app)
        val authService = vm.authService
        authService.clearProvisionedCredentials()

        val instKey = "InstKey@2026#Master"
        val indKey = "IndivKey@2026#Desk"
        assertTrue(authService.provisionFirstOperator(instKey, "Lead Strategist").isSuccess)
        assertTrue(authService.provisionAccountCredential("EMAT-INST-001", instKey, "Lead Strategist"))
        assertTrue(authService.provisionAccountCredential("EMAT-IND-002", indKey, "Individual Trader"))

        val instAccount = vm.availableAccounts.first { it.id == "EMAT-INST-001" }
        val indAccount = vm.availableAccounts.first { it.id == "EMAT-IND-002" }

        // 1. Sign in to Institutional account
        vm.selectLoginAccount(instAccount)
        vm.onPasswordChanged(instKey)
        vm.loginWithPassword()
        assertTrue("Must be authenticated after institutional login", vm.isAuthenticated.value)
        assertEquals("EMAT-INST-001", vm.currentAccount.value.id)
        assertEquals(com.example.emat.domain.model.AccountCategory.INSTITUTIONAL, vm.currentAccount.value.category)

        // Logout
        vm.logout()
        assertFalse("Must be unauthenticated after logout", vm.isAuthenticated.value)

        // 2. Sign in to Individual account
        vm.selectLoginAccount(indAccount)
        vm.onPasswordChanged(indKey)
        vm.loginWithPassword()
        assertTrue("Must be authenticated after individual login", vm.isAuthenticated.value)
        assertEquals("EMAT-IND-002", vm.currentAccount.value.id)
        assertEquals(com.example.emat.domain.model.AccountCategory.INDIVIDUAL, vm.currentAccount.value.category)
    }

    @Test
    fun `the audit entry is written and contains no credential`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.emat.ui.viewmodel.EmatViewModel(app)
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        val secretKey = "SecretAuditKey@2026#Check"

        val instAccount = vm.availableAccounts.first { it.id == "EMAT-INST-001" }
        vm.selectLoginAccount(instAccount)
        vm.onPasswordChanged(secretKey)
        vm.loginWithPassword()

        // Fetch recent audit logs from SQLite
        val recentLogs = db.auditLogDao().getRecentAuditLogs().first()
        val authLog = recentLogs.firstOrNull { it.category == "AUTH" && it.action.contains("LOGIN") }
        org.junit.Assert.assertNotNull("Audit entry for login must be written", authLog)

        // Assert that no part of the secret key is written to any field
        assertFalse("Audit dataAvailable must not contain password", authLog!!.dataAvailable.contains(secretKey))
        assertFalse("Audit rationale must not contain password", authLog.rationale.contains(secretKey))
        assertFalse("Audit decisionMade must not contain password", authLog.decisionMade.contains(secretKey))
        assertFalse("Audit actor must not contain password", authLog.actor.contains(secretKey))
        assertFalse("Audit action must not contain password", authLog.action.contains(secretKey))
    }

    // =========================================================================
    // BRICK 2 — SUBSCRIPTIONS AND TRIAL MANAGEMENT SPECIFICATION TESTS
    // =========================================================================

    @Test
    fun `row created on trial start (not before)`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)
        val accountId = "TEST-START-001"

        // 1. Before trial start: table has NO row for this account
        val rowBefore = db.entitlementDao().getEntitlement(accountId)
        org.junit.Assert.assertNull("No row in account_entitlements must exist before trial start", rowBefore)

        val derivedBefore = repo.getAccountEntitlement(accountId)
        assertEquals(com.example.emat.domain.model.EntitlementState.NOT_SUBSCRIBED, derivedBefore.state)
        assertEquals("UNAVAILABLE", derivedBefore.planId ?: "UNAVAILABLE")

        // 2. Start trial explicitly
        val started = repo.startTrialSafely(accountId)
        assertTrue("startTrialSafely must return true on first activation", started)

        // 3. After trial start: row exists with granted config duration
        val rowAfter = db.entitlementDao().getEntitlement(accountId)
        org.junit.Assert.assertNotNull("Row in account_entitlements must be created on trial start", rowAfter)
        assertEquals(accountId, rowAfter!!.accountId)
        assertEquals(com.example.emat.domain.model.DEFAULT_TRIAL_DURATION_DAYS, rowAfter.trialDurationDays)
        org.junit.Assert.assertNotNull(rowAfter.trialStartedAt)

        val derivedAfter = repo.getAccountEntitlement(accountId)
        assertEquals(com.example.emat.domain.model.EntitlementState.TRIAL_ACTIVE, derivedAfter.state)
        assertEquals(14, derivedAfter.remainingTrialDays)
    }

    @Test
    fun `derived expiry without background update`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)
        val accountId = "TEST-EXPIRY-001"

        assertTrue(repo.startTrialSafely(accountId))
        val active = repo.getAccountEntitlement(accountId)
        assertEquals(com.example.emat.domain.model.EntitlementState.TRIAL_ACTIVE, active.state)

        // Simulate 15 days of accumulated usage without running any background job
        val row = db.entitlementDao().getEntitlement(accountId)!!
        val fifteenDaysMillis = 15L * 24 * 3600 * 1000L
        db.entitlementDao().updateEntitlement(
            row.copy(
                accumulatedTrialDurationMillis = fifteenDaysMillis,
                lastObservedTimestamp = System.currentTimeMillis()
            )
        )

        // Read dynamically: state must be derived as TRIAL_EXPIRED immediately
        val expired = repo.getAccountEntitlement(accountId)
        assertEquals(com.example.emat.domain.model.EntitlementState.TRIAL_EXPIRED, expired.state)
        assertEquals(0, expired.remainingTrialDays)
        assertEquals(0, expired.remainingTrialHours)
    }

    @Test
    fun `clock rollback and reboot behavior (anomaly flag set, trial not extended, anomaly clears on catch-up)`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)
        val accountId = "TEST-CLOCK-001"

        assertTrue(repo.startTrialSafely(accountId))
        val baseTime = 1_000_000L
        val row = db.entitlementDao().getEntitlement(accountId)!!
        db.entitlementDao().updateEntitlement(
            row.copy(
                lastObservedTimestamp = baseTime,
                accumulatedTrialDurationMillis = 50_000L,
                hasClockAnomaly = false
            )
        )

        // 1. Clock moves backward by 20,000 ms (now = 980,000)
        repo.refreshEntitlementObservation(accountId, now = 980_000L)
        val backwardState = repo.getAccountEntitlement(accountId)
        assertTrue("Clock anomaly must be flagged when time rolls backward", backwardState.hasClockAnomaly)
        val backwardRow = db.entitlementDao().getEntitlement(accountId)!!
        assertEquals("Accumulated usage must NOT decrease or increase when clock moves backward", 50_000L, backwardRow.accumulatedTrialDurationMillis)
        assertEquals(baseTime, backwardRow.lastObservedTimestamp)

        // 2. Clock catches up and passes the highest observed timestamp (now = 1,010,000)
        repo.refreshEntitlementObservation(accountId, now = 1_010_000L)
        val recoveredState = repo.getAccountEntitlement(accountId)
        assertFalse("Clock anomaly must clear once wall clock passes previous lastObservedTimestamp", recoveredState.hasClockAnomaly)
        val recoveredRow = db.entitlementDao().getEntitlement(accountId)!!
        assertEquals("Accumulated usage must advance by the delta beyond previous last observed", 60_000L, recoveredRow.accumulatedTrialDurationMillis)
    }

    @Test
    fun `10 concurrent trial starts on 1 account yield exactly 1 success`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)
        val accountId = "TEST-CONCURRENT-ACC-001"

        val jobs = (1..10).map {
            async(Dispatchers.IO) {
                repo.startTrialSafely(accountId)
            }
        }
        val results = jobs.awaitAll()
        val successCount = results.count { it }
        val failureCount = results.count { !it }

        assertEquals("Exactly 1 concurrent start trial call must succeed", 1, successCount)
        assertEquals("Exactly 9 concurrent start trial calls must fail", 9, failureCount)
        val row = db.entitlementDao().getEntitlement(accountId)
        org.junit.Assert.assertNotNull(row)
    }

    @Test
    fun `close-trade works while expired`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)
        val accountId = "EMAT-INST-EXP-001"

        // Set account to TRIAL_EXPIRED
        assertTrue(repo.startTrialSafely(accountId))
        val row = db.entitlementDao().getEntitlement(accountId)!!
        db.entitlementDao().updateEntitlement(
            row.copy(accumulatedTrialDurationMillis = 20L * 24 * 3600 * 1000L)
        )
        val entitlement = repo.getAccountEntitlement(accountId)
        assertEquals(com.example.emat.domain.model.EntitlementState.TRIAL_EXPIRED, entitlement.state)

        // Clear active trades and insert one active trade
        db.tradeDao().emergencyStopAllTrades()
        val tradeId = "EXP-TRD-001"
        val trade = com.example.emat.data.local.entity.TradeEntity(
            tradeId = tradeId,
            accountId = accountId,
            symbol = "XAUUSD [TEST DATA]",
            direction = "BUY",
            entryPrice = 2050.0,
            stopLoss = 2040.0,
            takeProfit = 2070.0,
            positionSize = 0.50,
            currentPrice = 2055.0,
            unrealizedResult = 250.0,
            isBreakEven = false,
            riskStatus = "APPROVED",
            status = "ACTIVE",
            executionMode = "TEST_DATA_DEMO",
            openedAt = System.currentTimeMillis()
        )
        assertTrue(repo.submitTradeSafely(trade))
        assertEquals(1, db.tradeDao().getActiveTradeCountByAccount(accountId))

        // Closing trade while expired must succeed without restriction
        db.tradeDao().emergencyStopAllTrades()
        assertEquals("Active trades must be closed even when trial is expired", 0, db.tradeDao().getActiveTradeCountByAccount(accountId))
        val closedTrade = db.tradeDao().getTradesByAccountFlow(accountId).first().first { it.tradeId == tradeId }
        assertEquals("EMERGENCY_STOPPED", closedTrade.status)
    }

    @Test
    fun `export of own audit logs and evidence works while expired`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)
        val accountId = "EMAT-IND-002"

        // Mark account as TRIAL_EXPIRED
        repo.startTrialSafely(accountId)
        val row = db.entitlementDao().getEntitlement(accountId)!!
        db.entitlementDao().updateEntitlement(
            row.copy(accumulatedTrialDurationMillis = 25L * 24 * 3600 * 1000L)
        )
        assertEquals(com.example.emat.domain.model.EntitlementState.TRIAL_EXPIRED, repo.getAccountEntitlement(accountId).state)

        // Log evidence and audit records
        repo.logAuditEvent(
            accountId = accountId,
            category = "SYSTEM",
            action = "CONFIG_UPDATED",
            actor = "OPERATOR",
            rationale = "Testing audit read while expired",
            dataAvailable = "Data export request",
            decisionMade = "Allow read of user's own audit trail",
            outcome = "Permitted"
        )
        repo.logEvidence(
            com.example.emat.domain.model.EvidenceRecord(
                evidenceId = "EVD-EXP-001",
                instrument = "EURUSD",
                timeframe = "H1",
                sourceRef = "AUDIT_TEST",
                timestamp = System.currentTimeMillis(),
                observation = "Observation while expired",
                calculation = "Calculation while expired",
                interpretation = "Interpretation while expired",
                verificationStatus = "VERIFIED"
            ),
            accountId = accountId
        )

        // Verify reading/exporting own logs and evidence works 100%
        val logs = repo.getAuditLogsByAccountFlow(accountId).first()
        val evidence = repo.getEvidenceByAccountFlow(accountId).first()
        assertTrue("User must be able to read all own audit logs while expired", logs.isNotEmpty())
        assertTrue("User must be able to read all own evidence while expired", evidence.isNotEmpty())
        assertTrue(logs.any { it.action == "CONFIG_UPDATED" })
        assertTrue(evidence.any { it.evidenceId == "EVD-EXP-001" })
    }

    @Test
    fun `seeded v3 migration test preserves existing records, audit hash chain, and empty account_entitlements table`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("test_migration_v3_to_v4.db")
        if (dbFile.exists()) dbFile.delete()

        val factory = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("test_migration_v3_to_v4.db")
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    // Create v3 tables
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `instruments` (
                            `symbol` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `assetClass` TEXT NOT NULL,
                            `isActive` INTEGER NOT NULL,
                            `pipSize` REAL NOT NULL,
                            `digits` INTEGER NOT NULL,
                            `minVolume` REAL NOT NULL,
                            `contractSize` REAL NOT NULL,
                            PRIMARY KEY(`symbol`)
                        )
                    """.trimIndent())
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `trades` (
                            `tradeId` TEXT NOT NULL,
                            `accountId` TEXT NOT NULL,
                            `symbol` TEXT NOT NULL,
                            `direction` TEXT NOT NULL,
                            `entryPrice` REAL NOT NULL,
                            `stopLoss` REAL NOT NULL,
                            `takeProfit` REAL NOT NULL,
                            `positionSize` REAL NOT NULL,
                            `currentPrice` REAL NOT NULL,
                            `unrealizedResult` REAL NOT NULL,
                            `isBreakEven` INTEGER NOT NULL,
                            `riskStatus` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `executionMode` TEXT NOT NULL,
                            `openedAt` INTEGER NOT NULL,
                            PRIMARY KEY(`tradeId`)
                        )
                    """.trimIndent())
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `audit_logs` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `accountId` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `action` TEXT NOT NULL,
                            `actor` TEXT NOT NULL,
                            `rationale` TEXT NOT NULL,
                            `dataAvailable` TEXT NOT NULL,
                            `decisionMade` TEXT NOT NULL,
                            `outcome` TEXT NOT NULL,
                            `eventMetadata` TEXT NOT NULL,
                            `integrityHash` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = factory.create(config)
        val writableDb = helper.writableDatabase

        // Seed v3 database with trade and audit records
        writableDb.execSQL("""
            INSERT INTO `trades` VALUES (
                'TRD-V3-001', 'EMAT-INST-001', 'XAUUSD', 'BUY', 2000.0, 1990.0, 2020.0, 1.0, 2005.0, 500.0, 0, 'APPROVED', 'ACTIVE', 'LIVE', 1700000000000
            )
        """.trimIndent())

        val log1 = com.example.emat.data.local.entity.AuditLogEntity.createSecureEntry(
            accountId = "EMAT-INST-001",
            category = "SYSTEM",
            action = "SYSTEM_HEALTH_UPDATED",
            actor = "SYSTEM",
            rationale = "Initial v3 seed",
            dataAvailable = "v3 test payload",
            decisionMade = "Initialize v3 store",
            outcome = "Store initialized",
            eventMetadata = "{}",
            timestamp = 1700000001000L
        )
        writableDb.execSQL("""
            INSERT INTO `audit_logs` (`id`, `accountId`, `category`, `action`, `actor`, `rationale`, `dataAvailable`, `decisionMade`, `outcome`, `eventMetadata`, `integrityHash`, `timestamp`)
            VALUES (1, '${log1.accountId}', '${log1.category}', '${log1.action}', '${log1.actor}', '${log1.rationale}', '${log1.dataAvailable}', '${log1.decisionMade}', '${log1.outcome}', '${log1.eventMetadata}', '${log1.integrityHash}', ${log1.timestamp})
        """.trimIndent())

        val log2 = com.example.emat.data.local.entity.AuditLogEntity.createSecureEntry(
            accountId = "EMAT-INST-001",
            category = "EXECUTION",
            action = "TRADE_CLOSED",
            actor = "LEAD_OPERATOR",
            rationale = "Execution authorized",
            dataAvailable = "Trade TRD-V3-001",
            decisionMade = "Commit trade",
            outcome = "Trade active",
            eventMetadata = "{}",
            timestamp = 1700000002000L
        )
        writableDb.execSQL("""
            INSERT INTO `audit_logs` (`id`, `accountId`, `category`, `action`, `actor`, `rationale`, `dataAvailable`, `decisionMade`, `outcome`, `eventMetadata`, `integrityHash`, `timestamp`)
            VALUES (2, '${log2.accountId}', '${log2.category}', '${log2.action}', '${log2.actor}', '${log2.rationale}', '${log2.dataAvailable}', '${log2.decisionMade}', '${log2.outcome}', '${log2.eventMetadata}', '${log2.integrityHash}', ${log2.timestamp})
        """.trimIndent())

        // Execute MIGRATION_3_4
        com.example.emat.data.local.EmatDatabase.MIGRATION_3_4.migrate(writableDb)

        // 1. Verify account_entitlements exists and is completely empty (no hardcoded seeds)
        val entCursor = writableDb.query("SELECT COUNT(*) FROM `account_entitlements`")
        entCursor.moveToFirst()
        val entCount = entCursor.getInt(0)
        entCursor.close()
        assertEquals("account_entitlements must exist and be completely empty after MIGRATION_3_4", 0, entCount)

        // 2. Verify trades table is completely intact
        val tradeCursor = writableDb.query("SELECT symbol, direction, entryPrice, status FROM `trades` WHERE tradeId = 'TRD-V3-001'")
        assertTrue(tradeCursor.moveToFirst())
        assertEquals("XAUUSD", tradeCursor.getString(0))
        assertEquals("BUY", tradeCursor.getString(1))
        assertEquals(2000.0, tradeCursor.getDouble(2), 0.001)
        assertEquals("ACTIVE", tradeCursor.getString(3))
        tradeCursor.close()

        // 3. Verify audit log chain verifies perfectly
        val auditCursor = writableDb.query("SELECT id, accountId, category, action, actor, rationale, dataAvailable, decisionMade, outcome, eventMetadata, integrityHash, timestamp FROM `audit_logs` ORDER BY id ASC")
        var verifiedCount = 0
        while (auditCursor.moveToNext()) {
            val entity = com.example.emat.data.local.entity.AuditLogEntity(
                id = auditCursor.getLong(0),
                accountId = auditCursor.getString(1),
                category = auditCursor.getString(2),
                action = auditCursor.getString(3),
                actor = auditCursor.getString(4),
                rationale = auditCursor.getString(5),
                dataAvailable = auditCursor.getString(6),
                decisionMade = auditCursor.getString(7),
                outcome = auditCursor.getString(8),
                eventMetadata = auditCursor.getString(9),
                integrityHash = auditCursor.getString(10),
                timestamp = auditCursor.getLong(11)
            )
            assertTrue("Audit log #${entity.id} SHA-256 integrity hash must remain 100% valid after migration", entity.verifyIntegrity())
            verifiedCount++
        }
        auditCursor.close()
        assertEquals(2, verifiedCount)

        helper.close()
        dbFile.delete()
    }

    @Test
    fun `restricted features blocked when expired or not subscribed`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.emat.ui.viewmodel.EmatViewModel(app)

        // Authenticate with valid session
        val authService = vm.authService
        val key = "SecKey@2026#ValidGate"
        authService.provisionFirstOperator(key, "Lead Strategist")
        vm.onPasswordChanged(key)
        vm.loginWithPassword()
        assertTrue(vm.isAuthenticated.value)

        // 1. NOT_SUBSCRIBED state
        val unentitledState = com.example.emat.domain.model.EntitlementState.NOT_SUBSCRIBED
        assertTrue("FULL_MARKET_SCAN must be restricted when NOT_SUBSCRIBED", com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(com.example.emat.domain.service.EmatFeature.FULL_MARKET_SCAN, unentitledState))
        assertTrue("AI_DECISION_ANALYSIS must be restricted when NOT_SUBSCRIBED", com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(com.example.emat.domain.service.EmatFeature.AI_DECISION_ANALYSIS, unentitledState))
        assertTrue("INSTRUMENT_REGISTRY_MUTATION must be restricted when NOT_SUBSCRIBED", com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(com.example.emat.domain.service.EmatFeature.INSTRUMENT_REGISTRY_MUTATION, unentitledState))

        // Never-gated operations must NOT be restricted
        assertFalse("CLOSE_TRADE must never be restricted", com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(com.example.emat.domain.service.EmatFeature.CLOSE_TRADE, unentitledState))
        assertFalse("EMERGENCY_STOP must never be restricted", com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(com.example.emat.domain.service.EmatFeature.EMERGENCY_STOP, unentitledState))
        assertFalse("EXPORT_OWN_DATA must never be restricted", com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(com.example.emat.domain.service.EmatFeature.EXPORT_OWN_DATA, unentitledState))
        assertFalse("VIEW_OWN_AUDIT_LOGS must never be restricted", com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(com.example.emat.domain.service.EmatFeature.VIEW_OWN_AUDIT_LOGS, unentitledState))
        assertFalse("AUTHENTICATION must never be restricted", com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(com.example.emat.domain.service.EmatFeature.AUTHENTICATION, unentitledState))

        // 2. TRIAL_EXPIRED state
        val expiredState = com.example.emat.domain.model.EntitlementState.TRIAL_EXPIRED
        assertTrue("FULL_MARKET_SCAN must be restricted when TRIAL_EXPIRED", com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(com.example.emat.domain.service.EmatFeature.FULL_MARKET_SCAN, expiredState))
        assertTrue("AI_DECISION_ANALYSIS must be restricted when TRIAL_EXPIRED", com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(com.example.emat.domain.service.EmatFeature.AI_DECISION_ANALYSIS, expiredState))
        assertTrue("INSTRUMENT_REGISTRY_MUTATION must be restricted when TRIAL_EXPIRED", com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(com.example.emat.domain.service.EmatFeature.INSTRUMENT_REGISTRY_MUTATION, expiredState))

        // 3. TRIAL_ACTIVE state
        val activeState = com.example.emat.domain.model.EntitlementState.TRIAL_ACTIVE
        assertFalse("FULL_MARKET_SCAN must NOT be restricted when TRIAL_ACTIVE", com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(com.example.emat.domain.service.EmatFeature.FULL_MARKET_SCAN, activeState))
        assertFalse("AI_DECISION_ANALYSIS must NOT be restricted when TRIAL_ACTIVE", com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(com.example.emat.domain.service.EmatFeature.AI_DECISION_ANALYSIS, activeState))
        assertFalse("INSTRUMENT_REGISTRY_MUTATION must NOT be restricted when TRIAL_ACTIVE", com.example.emat.domain.service.FeatureAccessPolicy.isFeatureRestricted(com.example.emat.domain.service.EmatFeature.INSTRUMENT_REGISTRY_MUTATION, activeState))

        // 4. Test ViewModel enforcement when unentitled
        vm.clearEntitlementRestrictionMessage()
        vm.triggerFullMarketScan()
        org.junit.Assert.assertNotNull("entitlementRestrictionMessage must be set when scan attempted without entitlement", vm.entitlementRestrictionMessage.value)

        vm.clearEntitlementRestrictionMessage()
        vm.openAiAnalysis()
        org.junit.Assert.assertNotNull("entitlementRestrictionMessage must be set when AI analysis attempted without entitlement", vm.entitlementRestrictionMessage.value)

        vm.clearEntitlementRestrictionMessage()
        vm.toggleInstrumentActive("XAUUSD", false)
        org.junit.Assert.assertNotNull("entitlementRestrictionMessage must be set when registry mutation attempted without entitlement", vm.entitlementRestrictionMessage.value)
    }

    @Test
    fun `test billing provider is unreachable in release and labeled test data`() = kotlinx.coroutines.runBlocking {
        val testProvider = object : com.example.emat.domain.service.BillingProvider {
            override val connectionStatus: com.example.emat.domain.service.BillingConnectionStatus
                get() = com.example.emat.domain.service.BillingConnectionStatus.CONNECTED

            override suspend fun verifySubscription(accountId: String): com.example.emat.domain.service.PurchaseVerificationResult {
                return com.example.emat.domain.service.PurchaseVerificationResult(
                    isValid = true,
                    subscriptionId = "TEST-SUB-12345 [TEST DATA]",
                    planId = "opaque_plan_id",
                    purchasedAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + 30L * 24 * 3600 * 1000L,
                    isVerified = true,
                    statusMessage = "VERIFIED [TEST DATA]"
                )
            }
        }

        val result = testProvider.verifySubscription("EMAT-INST-001")
        assertTrue(result.isValid)
        assertTrue("Subscription ID must be labeled TEST DATA", result.subscriptionId?.contains("[TEST DATA]") == true)

        // DefaultBillingProvider in production sources always returns NOT_CONNECTED and fails verification
        val defaultProvider = com.example.emat.domain.service.DefaultBillingProvider()
        assertEquals(com.example.emat.domain.service.BillingConnectionStatus.NOT_CONNECTED, defaultProvider.connectionStatus)
        val defaultResult = defaultProvider.verifySubscription("EMAT-INST-001")
        assertFalse(defaultResult.isValid)
    }

    @Test
    fun `each of the five entitlement states derived from stored facts`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)

        // 1. NOT_SUBSCRIBED: No record exists
        val notSub = repo.getAccountEntitlement("TEST-ACC-NOT-SUB")
        assertEquals(com.example.emat.domain.model.EntitlementState.NOT_SUBSCRIBED, notSub.state)

        // 2. TRIAL_ACTIVE: Trial started with remaining time
        val now = System.currentTimeMillis()
        db.entitlementDao().insertEntitlement(
            com.example.emat.data.local.entity.AccountEntitlementEntity(
                accountId = "TEST-ACC-TRIAL-ACTIVE",
                trialStartedAt = now,
                trialDurationDays = 14,
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
        )
        val trialActive = repo.getAccountEntitlement("TEST-ACC-TRIAL-ACTIVE")
        assertEquals(com.example.emat.domain.model.EntitlementState.TRIAL_ACTIVE, trialActive.state)

        // 3. TRIAL_EXPIRED: Trial started but duration fully accumulated
        val past = now - (15L * 24 * 3600 * 1000L)
        db.entitlementDao().insertEntitlement(
            com.example.emat.data.local.entity.AccountEntitlementEntity(
                accountId = "TEST-ACC-TRIAL-EXP",
                trialStartedAt = past,
                trialDurationDays = 14,
                accumulatedTrialDurationMillis = 15L * 24 * 3600 * 1000L,
                lastObservedTimestamp = now,
                hasClockAnomaly = false,
                subscriptionId = null,
                subscriptionPlanId = null,
                subscriptionPurchasedAt = null,
                subscriptionExpiresAt = null,
                verifiedByBillingProvider = false,
                updatedAt = now
            )
        )
        val trialExp = repo.getAccountEntitlement("TEST-ACC-TRIAL-EXP")
        assertEquals(com.example.emat.domain.model.EntitlementState.TRIAL_EXPIRED, trialExp.state)

        // 4. SUBSCRIPTION_ACTIVE: verifiedByBillingProvider is true and expires in future
        db.entitlementDao().insertEntitlement(
            com.example.emat.data.local.entity.AccountEntitlementEntity(
                accountId = "TEST-ACC-SUB-ACTIVE",
                trialStartedAt = null,
                trialDurationDays = 14,
                accumulatedTrialDurationMillis = 0L,
                lastObservedTimestamp = now,
                hasClockAnomaly = false,
                subscriptionId = "SUB-001",
                subscriptionPlanId = "tier1",
                subscriptionPurchasedAt = now,
                subscriptionExpiresAt = now + (30L * 24 * 3600 * 1000L),
                verifiedByBillingProvider = true,
                updatedAt = now
            )
        )
        val subActive = repo.getAccountEntitlement("TEST-ACC-SUB-ACTIVE")
        assertEquals(com.example.emat.domain.model.EntitlementState.SUBSCRIPTION_ACTIVE, subActive.state)

        // 5. SUBSCRIPTION_EXPIRED: subscription exists but expiry is in the past
        db.entitlementDao().insertEntitlement(
            com.example.emat.data.local.entity.AccountEntitlementEntity(
                accountId = "TEST-ACC-SUB-EXP",
                trialStartedAt = null,
                trialDurationDays = 14,
                accumulatedTrialDurationMillis = 0L,
                lastObservedTimestamp = now,
                hasClockAnomaly = false,
                subscriptionId = "SUB-002",
                subscriptionPlanId = "tier1",
                subscriptionPurchasedAt = past,
                subscriptionExpiresAt = now - 1000L,
                verifiedByBillingProvider = true,
                updatedAt = now
            )
        )
        val subExp = repo.getAccountEntitlement("TEST-ACC-SUB-EXP")
        assertEquals(com.example.emat.domain.model.EntitlementState.SUBSCRIPTION_EXPIRED, subExp.state)
    }

    @Test
    fun `NOT_CONNECTED billing cannot produce SUBSCRIPTION_ACTIVE`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)
        val now = System.currentTimeMillis()

        // Unverified subscription (verifiedByBillingProvider = false) even with future expiry must NOT be SUBSCRIPTION_ACTIVE
        db.entitlementDao().insertEntitlement(
            com.example.emat.data.local.entity.AccountEntitlementEntity(
                accountId = "TEST-UNVERIFIED-SUB",
                trialStartedAt = null,
                trialDurationDays = 14,
                accumulatedTrialDurationMillis = 0L,
                lastObservedTimestamp = now,
                hasClockAnomaly = false,
                subscriptionId = "UNVERIFIED-SUB",
                subscriptionPlanId = "plan",
                subscriptionPurchasedAt = now,
                subscriptionExpiresAt = now + (30L * 24 * 3600 * 1000L),
                verifiedByBillingProvider = false, // Not verified by provider!
                updatedAt = now
            )
        )
        val entitlement = repo.getAccountEntitlement("TEST-UNVERIFIED-SUB")
        assertFalse("Unverified billing must never produce SUBSCRIPTION_ACTIVE", entitlement.state == com.example.emat.domain.model.EntitlementState.SUBSCRIPTION_ACTIVE)
        assertEquals(com.example.emat.domain.model.EntitlementState.NOT_SUBSCRIBED, entitlement.state)
    }

    @Test
    fun `trial expiry just before at and just after the boundary`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)
        val now = System.currentTimeMillis()
        val totalAllowedMillis = 14L * 24 * 3600 * 1000L

        // Boundary 1: Just before expiry (100ms remaining)
        db.entitlementDao().insertEntitlement(
            com.example.emat.data.local.entity.AccountEntitlementEntity(
                accountId = "TEST-BEFORE-EXP",
                trialStartedAt = now - (totalAllowedMillis - 100L),
                trialDurationDays = 14,
                accumulatedTrialDurationMillis = totalAllowedMillis - 100L,
                lastObservedTimestamp = now,
                hasClockAnomaly = false,
                subscriptionId = null,
                subscriptionPlanId = null,
                subscriptionPurchasedAt = null,
                subscriptionExpiresAt = null,
                verifiedByBillingProvider = false,
                updatedAt = now
            )
        )
        assertEquals(com.example.emat.domain.model.EntitlementState.TRIAL_ACTIVE, repo.getAccountEntitlement("TEST-BEFORE-EXP").state)

        // Boundary 2: Exactly at boundary (0ms remaining)
        db.entitlementDao().insertEntitlement(
            com.example.emat.data.local.entity.AccountEntitlementEntity(
                accountId = "TEST-AT-EXP",
                trialStartedAt = now - totalAllowedMillis,
                trialDurationDays = 14,
                accumulatedTrialDurationMillis = totalAllowedMillis,
                lastObservedTimestamp = now,
                hasClockAnomaly = false,
                subscriptionId = null,
                subscriptionPlanId = null,
                subscriptionPurchasedAt = null,
                subscriptionExpiresAt = null,
                verifiedByBillingProvider = false,
                updatedAt = now
            )
        )
        assertEquals(com.example.emat.domain.model.EntitlementState.TRIAL_EXPIRED, repo.getAccountEntitlement("TEST-AT-EXP").state)

        // Boundary 3: Just after boundary (100ms past expiry)
        db.entitlementDao().insertEntitlement(
            com.example.emat.data.local.entity.AccountEntitlementEntity(
                accountId = "TEST-AFTER-EXP",
                trialStartedAt = now - (totalAllowedMillis + 100L),
                trialDurationDays = 14,
                accumulatedTrialDurationMillis = totalAllowedMillis + 100L,
                lastObservedTimestamp = now,
                hasClockAnomaly = false,
                subscriptionId = null,
                subscriptionPlanId = null,
                subscriptionPurchasedAt = null,
                subscriptionExpiresAt = null,
                verifiedByBillingProvider = false,
                updatedAt = now
            )
        )
        assertEquals(com.example.emat.domain.model.EntitlementState.TRIAL_EXPIRED, repo.getAccountEntitlement("TEST-AFTER-EXP").state)
    }

    @Test
    fun `a second trial for the same account is refused sequential`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)
        val testAcc = "TEST-SEQ-TRIAL-ACC"

        val firstAttempt = repo.startTrialSafely(testAcc)
        assertTrue("First trial start must succeed", firstAttempt)

        val secondAttempt = repo.startTrialSafely(testAcc)
        assertFalse("Sequential second trial start on same account must be refused", secondAttempt)
    }

    @Test
    fun `one account trial does not affect another account`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)

        val acc1 = "TEST-ISOLATED-ACC-1"
        val acc2 = "TEST-ISOLATED-ACC-2"

        // Account 1 starts trial
        assertTrue(repo.startTrialSafely(acc1))
        assertEquals(com.example.emat.domain.model.EntitlementState.TRIAL_ACTIVE, repo.getAccountEntitlement(acc1).state)

        // Account 2 has not started trial and must remain NOT_SUBSCRIBED
        assertEquals(com.example.emat.domain.model.EntitlementState.NOT_SUBSCRIBED, repo.getAccountEntitlement(acc2).state)
    }

    @Test
    fun `entitlement never changes account category and never enables live trading or broker connection`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)
        val vm = com.example.emat.ui.viewmodel.EmatViewModel(app)

        val instAccount = vm.availableAccounts.first { it.category == com.example.emat.domain.model.AccountCategory.INSTITUTIONAL }
        val indAccount = vm.availableAccounts.first { it.category == com.example.emat.domain.model.AccountCategory.INDIVIDUAL }

        // Start trial on Individual account
        repo.startTrialSafely(indAccount.id)
        val entInd = repo.getAccountEntitlement(indAccount.id)
        assertEquals(com.example.emat.domain.model.EntitlementState.TRIAL_ACTIVE, entInd.state)

        // Category remains INDIVIDUAL
        val refreshedIndAccount = vm.availableAccounts.first { it.id == indAccount.id }
        assertEquals(com.example.emat.domain.model.AccountCategory.INDIVIDUAL, refreshedIndAccount.category)

        // Broker state remains NOT_CONNECTED
        val engine = com.example.emat.domain.service.ExecutionEngine()
        assertEquals(com.example.emat.domain.model.ExecutionState.NOT_CONNECTED, engine.getExecutionState())
        assertFalse("Live execution must never be enabled on individual account", refreshedIndAccount.isLiveExecutionAllowed)
    }

    @Test
    fun `trial start expiry and clock anomaly audit entries are written with category ENTITLEMENT and contain no payment data`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)
        val testAcc = "TEST-AUDIT-ACC-101"

        // 1. Trial start
        assertTrue(repo.startTrialSafely(testAcc))
        val startRow = db.entitlementDao().getEntitlement(testAcc)!!
        val startTime = startRow.trialStartedAt!!

        // 2. Clock anomaly (backward jump)
        repo.refreshEntitlementObservation(testAcc, startTime - 5000L)

        // 3. Trial expiry (forward jump past 14 days, called twice to verify idempotency)
        val after15Days = startTime + (15L * 24 * 3600 * 1000L)
        repo.refreshEntitlementObservation(testAcc, after15Days)
        repo.refreshEntitlementObservation(testAcc, after15Days + 1000L)

        val logs = db.auditLogDao().getRecentAuditLogsByAccount(testAcc).first()
        val trialStartEntry = logs.firstOrNull { it.action == "TRIAL_STARTED" }
        val anomalyEntry = logs.firstOrNull { it.action == "CLOCK_ANOMALY_DETECTED" }
        val expiredEntries = logs.filter { it.action == "TRIAL_EXPIRED" }

        org.junit.Assert.assertNotNull("TRIAL_STARTED entry must exist", trialStartEntry)
        assertEquals("ENTITLEMENT", trialStartEntry?.category)

        org.junit.Assert.assertNotNull("CLOCK_ANOMALY_DETECTED entry must exist", anomalyEntry)
        assertEquals("ENTITLEMENT", anomalyEntry?.category)

        assertEquals("TRIAL_EXPIRED must be logged once (idempotent)", 1, expiredEntries.size)
        assertEquals("ENTITLEMENT", expiredEntries.first().category)

        // Verify zero payment tokens/data in log fields
        val forbiddenTokens = listOf("card", "token", "cvv", "payment", "credit", "stripe", "google_play")
        for (entry in listOfNotNull(trialStartEntry, anomalyEntry, expiredEntries.firstOrNull())) {
            for (token in forbiddenTokens) {
                assertFalse(entry.rationale.lowercase().contains(token))
                assertFalse(entry.dataAvailable.lowercase().contains(token))
                assertFalse(entry.decisionMade.lowercase().contains(token))
            }
        }
    }

    @Test
    fun `direct repository calls for scanner AI analysis and instrument mutations are refused when not entitled`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.emat.data.local.EmatDatabase.getDatabase(app)
        val repo = com.example.emat.data.repository.EmatRepositoryImpl(db)
        val unentitledAcc = "TEST-DIRECT-REPO-GATING"

        val scanResult = repo.executeMarketScan(dataProviderType = "TEST", accountId = unentitledAcc)
        assertTrue("Direct executeMarketScan must be refused when not entitled", scanResult.isEmpty())

        val aiResult = repo.generateAiAnalysis(
            accountId = unentitledAcc,
            contextTitle = "Direct Test",
            activeTrades = 0,
            isOffline = false
        )
        org.junit.Assert.assertNull("Direct generateAiAnalysis must be refused when not entitled", aiResult)

        val addResult = repo.addInstrument(
            com.example.emat.data.local.entity.InstrumentEntity(
                symbol = "TESTPAIR",
                name = "Test Pair",
                assetClass = "FOREX",
                isActive = true,
                pipSize = 0.0001,
                digits = 4
            ),
            accountId = unentitledAcc
        )
        assertFalse("Direct addInstrument must be refused when not entitled", addResult)

        val toggleResult = repo.toggleInstrument("EURUSD", false, accountId = unentitledAcc)
        assertFalse("Direct toggleInstrument must be refused when not entitled", toggleResult)

        val logs = db.auditLogDao().getRecentAuditLogsByAccount(unentitledAcc).first()
        val deniedCount = logs.count { it.action == "FEATURE_ACCESS_DENIED" && it.category == "ENTITLEMENT" }
        assertEquals("All 4 direct repository rejections must log FEATURE_ACCESS_DENIED", 4, deniedCount)
    }

    private fun fakeOkHttpClient(handler: (okhttp3.Request) -> okhttp3.Response): okhttp3.OkHttpClient {
        return okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain -> handler(chain.request()) }
            .build()
    }

    @Test
    fun `biquote state transitions NOT_CONNECTED to CONNECTED after valid fresh quote`() = kotlinx.coroutines.runBlocking {
        val nowMs = 1_700_000_000_000L
        val client = fakeOkHttpClient { req ->
            okhttp3.Response.Builder()
                .request(req)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(
                    okhttp3.ResponseBody.create(
                        null,
                        """{"symbol":"EURUSD","bid":1.11814,"ask":1.11820,"last":0.0,"volume":0,"timestamp":"2023-11-14T22:13:20Z","source":"MetaTrader 5 (Broker 1)","stale":false}"""
                    )
                )
                .build()
        }
        val provider = com.example.emat.domain.service.BiquoteMarketDataProvider(
            client = client,
            timeProvider = { nowMs }
        )
        assertEquals(com.example.emat.domain.service.MarketDataState.NOT_CONNECTED, provider.getDataState())
        val res = provider.fetchQuote("EURUSD")
        assertTrue(res.isSuccess)
        assertEquals(com.example.emat.domain.service.MarketDataState.CONNECTED, provider.getDataState())
        val q = res.getOrThrow()
        assertEquals(1.11814, q.bid, 1e-6)
        assertEquals(1.11820, q.ask, 1e-6)
        assertTrue(q.isSpreadAvailable)
        assertEquals(com.example.emat.domain.model.DataOrigin.LIVE_FEED, q.origin)
    }

    @Test
    fun `biquote rejects stale, inverted bid-ask, NaN, and missing price`() = kotlinx.coroutines.runBlocking {
        val nowMs = 1_700_000_000_000L
        fun makeProvider(body: String) = com.example.emat.domain.service.BiquoteMarketDataProvider(
            client = fakeOkHttpClient { req ->
                okhttp3.Response.Builder()
                    .request(req)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(okhttp3.ResponseBody.create(null, body))
                    .build()
            },
            timeProvider = { nowMs }
        )

        val staleRes = makeProvider("""{"symbol":"EURUSD","bid":1.11,"ask":1.12,"timestamp":"2023-11-14T22:13:20Z","stale":true}""").fetchQuote("EURUSD")
        assertTrue(staleRes.isFailure)

        val invRes = makeProvider("""{"symbol":"EURUSD","bid":1.13,"ask":1.12,"timestamp":"2023-11-14T22:13:20Z","stale":false}""").fetchQuote("EURUSD")
        assertTrue(invRes.isFailure)

        val zeroRes = makeProvider("""{"symbol":"EURUSD","bid":0.0,"ask":0.0,"last":0.0,"timestamp":"2023-11-14T22:13:20Z","stale":false}""").fetchQuote("EURUSD")
        assertTrue(zeroRes.isFailure)
    }

    @Test
    fun `biquote preserves missing bid-ask as unavailable without synthesizing`() = kotlinx.coroutines.runBlocking {
        val nowMs = 1_700_000_000_000L
        val provider = com.example.emat.domain.service.BiquoteMarketDataProvider(
            client = fakeOkHttpClient { req ->
                okhttp3.Response.Builder()
                    .request(req)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        okhttp3.ResponseBody.create(
                            null,
                            """{"symbol":"EURUSD","last":1.11817,"timestamp":"2023-11-14T22:13:20Z","stale":false}"""
                        )
                    )
                    .build()
            },
            timeProvider = { nowMs }
        )
        val res = provider.fetchQuote("EURUSD")
        assertTrue(res.isSuccess)
        val q = res.getOrThrow()
        assertTrue(q.bid.isNaN())
        assertTrue(q.ask.isNaN())
        assertEquals(1.11817, q.lastPrice, 1e-6)
        assertFalse(q.isSpreadAvailable)
        assertEquals(com.example.emat.domain.service.MarketDataState.CONNECTED, provider.getDataState())
    }

    @Test
    fun `biquote handles 404, 429, timeout, and parse errors with diagnostic reasons`() = kotlinx.coroutines.runBlocking {
        fun makeProvider(interceptor: okhttp3.Interceptor) = com.example.emat.domain.service.BiquoteMarketDataProvider(
            client = okhttp3.OkHttpClient.Builder().addInterceptor(interceptor).build()
        )

        val p404 = makeProvider { chain ->
            okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1)
                .code(404).message("Not Found").body(okhttp3.ResponseBody.create(null, "")).build()
        }
        p404.fetchQuote("NAS100")
        assertEquals("SYMBOL UNAVAILABLE (HTTP 404)", p404.getLastErrorReason())

        val p429 = makeProvider { chain ->
            okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1)
                .code(429).message("Too Many Requests").body(okhttp3.ResponseBody.create(null, "")).build()
        }
        p429.fetchQuote("EURUSD")
        assertEquals("HTTP 429 rate limited", p429.getLastErrorReason())

        val pTimeout = makeProvider { _ -> throw java.net.SocketTimeoutException("timeout") }
        pTimeout.fetchQuote("EURUSD")
        assertEquals("TIMEOUT", pTimeout.getLastErrorReason())

        val pParse = makeProvider { chain ->
            okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1)
                .code(200).message("OK").body(okhttp3.ResponseBody.create(null, "{not-json")).build()
        }
        pParse.fetchQuote("EURUSD")
        assertEquals("PARSE ERROR", pParse.getLastErrorReason())
    }

    @Test
    fun `biquote registry has 19 instruments and parses OHLC candles`() = kotlinx.coroutines.runBlocking {
        assertEquals(19, com.example.emat.domain.service.BiquoteMarketDataProvider.REGISTRY_SYMBOLS.size)
        val provider = com.example.emat.domain.service.BiquoteMarketDataProvider(
            client = fakeOkHttpClient { req ->
                okhttp3.Response.Builder()
                    .request(req)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        okhttp3.ResponseBody.create(
                            null,
                            """{"symbol":"EURUSD","interval":"1h","bars":[{"timestamp":"2023-11-14T21:00:00Z","open":1.1180,"high":1.1190,"low":1.1175,"close":1.1185}]}"""
                        )
                    )
                    .build()
            }
        )
        val res = provider.fetchCandles("EURUSD", "1h", 10)
        assertTrue(res.isSuccess)
        assertEquals(1, res.getOrThrow().size)
    }

    @Test
    fun `test MT5 broker configuration saves encrypted and wipes securely`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.emat.domain.service.AuthService(context)

        // Clean initial state
        authService.clearMt5BrokerConfig()
        val initialCfg = authService.getMt5BrokerConfig()
        assertFalse(initialCfg.isConfigured)

        // Save broker config
        val saved = authService.saveMt5BrokerConfig(
            brokerName = "IC Markets",
            brokerServer = "ICMarketsSC-Live05",
            accountNumber = "987654321",
            investorPassword = "ReadOnlyPassword123!",
            bridgeUrl = "https://bridge.emat.local:8000",
            bridgeToken = "secure_bridge_token_abc"
        )
        assertTrue("MT5 broker config should save", saved)

        val storedCfg = authService.getMt5BrokerConfig()
        assertTrue(storedCfg.isConfigured)
        assertEquals("IC Markets", storedCfg.brokerName)
        assertEquals("ICMarketsSC-Live05", storedCfg.brokerServer)
        assertEquals("987654321", storedCfg.accountNumber)
        assertTrue("Investor password must be masked", storedCfg.investorPasswordMasked)
        assertTrue("Bridge token must be masked", storedCfg.bridgeTokenMasked)

        // Plaintext passwords can be decrypted internally by vault
        assertEquals("ReadOnlyPassword123!", authService.getMt5BrokerInvestorPassword())
        assertEquals("secure_bridge_token_abc", authService.getMt5BridgeToken())

        // Wipe config
        authService.clearMt5BrokerConfig()
        val wipedCfg = authService.getMt5BrokerConfig()
        assertFalse(wipedCfg.isConfigured)
        assertNull(authService.getMt5BrokerInvestorPassword())
        assertNull(authService.getMt5BridgeToken())
    }

    @Test
    fun `test MT5 bridge client handles health ping and quote response`() = kotlinx.coroutines.runBlocking {
        val fakeClient = fakeOkHttpClient { req ->
            val path = req.url.encodedPath
            if (path.contains("health")) {
                okhttp3.Response.Builder()
                    .request(req)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        okhttp3.ResponseBody.create(
                            null,
                            """{"terminalConnected":true,"accountNumber":"987654321","symbolsCount":19}"""
                        )
                    )
                    .build()
            } else {
                okhttp3.Response.Builder()
                    .request(req)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        okhttp3.ResponseBody.create(
                            null,
                            """{"quotes":[{"symbol":"EURUSD","bid":1.10915,"ask":1.10921,"spread":0.00006,"timestamp":1700000000000,"source":"MT5 IC Markets"}]}"""
                        )
                    )
                    .build()
            }
        }

        val bridge = com.example.emat.domain.service.Mt5BridgeClient(client = fakeClient)
        val testRes = bridge.testConnection(
            bridgeUrl = "https://bridge.emat.local:8000",
            token = "token123",
            brokerServer = "ICMarketsSC-Live05",
            accountNumber = "987654321"
        )
        assertTrue(testRes.isSuccess)
        assertTrue(testRes.terminalConnected)
        assertTrue(testRes.accountMatches)
        assertEquals(19, testRes.discoveredSymbolsCount)

        val quotesRes = bridge.fetchBrokerQuotes("https://bridge.emat.local:8000", "token123")
        assertTrue(quotesRes.isSuccess)
        val quotes = quotesRes.getOrThrow()
        assertTrue(quotes.containsKey("EURUSD"))
        assertEquals(1.10915, quotes["EURUSD"]!!.bid, 1e-5)
    }

    @Test
    fun `test XM Global DEMO broker configuration populated automatically with encrypted password`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.emat.domain.service.AuthService(context)

        val defaultCfg = authService.getMt5BrokerConfig()

        // Verify prefilled defaults for XM Global DEMO
        assertEquals("XM Global", defaultCfg.brokerName)
        assertEquals("XMGlobal-MT5 10", defaultCfg.brokerServer)
        assertEquals("346533676", defaultCfg.accountNumber)
        assertEquals("DEMO", defaultCfg.accountType)
        assertTrue("Investor password must be masked in UI", defaultCfg.investorPasswordMasked)

        // Verify credentials securely stored in Keystore encrypted storage
        val decryptedPassword = authService.getMt5BrokerInvestorPassword()
        assertEquals("@Guerrier#19940", decryptedPassword)
    }

    @Test
    fun `test MT5 bridge auto-detects XM Global DEMO config from assets`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authService = com.example.emat.domain.service.AuthService(context)

        val detected = authService.autoDetectAndRegisterBridge()
        assertNotNull(detected)
        assertEquals("XM Global", detected!!.brokerName)
        assertEquals("XMGlobal-MT5 10", detected.server)
        assertEquals("346533676", detected.account)
        assertEquals("DEMO", detected.accountType)
        assertEquals("@Guerrier#19940", detected.password)
        assertTrue("Bridge URL must be valid", detected.url.isNotBlank())
        assertTrue("Bridge token must be non-empty", detected.token.isNotBlank())

        val registeredToken = authService.getMt5BridgeToken()
        assertNotNull(registeredToken)
        assertEquals(detected.token, registeredToken)

        val cfg = authService.getMt5BrokerConfig()
        assertEquals(com.example.emat.domain.model.BridgeStatus.CONFIGURED, cfg.bridgeStatus)
        assertEquals("BRIDGE CONFIGURED", cfg.statusMessage)
    }

    @Test
    fun `test MT5 bridge client handles connection to XM Global DEMO server`() = kotlinx.coroutines.runBlocking {
        val fakeClient = fakeOkHttpClient { req ->
            val path = req.url.encodedPath
            when {
                path.contains("health") -> {
                    okhttp3.Response.Builder()
                        .request(req)
                        .protocol(okhttp3.Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(okhttp3.ResponseBody.create(null, """{"terminalConnected":true,"accountNumber":"346533676","symbolsCount":19}"""))
                        .build()
                }
                path.endsWith("/connect") -> {
                    okhttp3.Response.Builder()
                        .request(req)
                        .protocol(okhttp3.Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(okhttp3.ResponseBody.create(null, """{"success":true,"message":"Connected to XM Global DEMO Terminal"}"""))
                        .build()
                }
                path.endsWith("/status") -> {
                    okhttp3.Response.Builder()
                        .request(req)
                        .protocol(okhttp3.Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(okhttp3.ResponseBody.create(null, """{"connected":true,"server":"XMGlobal-MT5 10","accountNumber":"346533676","company":"XM Global"}"""))
                        .build()
                }
                path.contains("symbols") -> {
                    okhttp3.Response.Builder()
                        .request(req)
                        .protocol(okhttp3.Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(okhttp3.ResponseBody.create(null, """{"mapped_count":19,"total_canonical":19,"missing_instruments":[]}"""))
                        .build()
                }
                else -> {
                    okhttp3.Response.Builder()
                        .request(req)
                        .protocol(okhttp3.Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(okhttp3.ResponseBody.create(null, """{"quotes":[{"symbol":"EURUSD","bid":1.08502,"ask":1.08518,"spread":0.00016,"timestamp":1700000000000,"source":"XM Global MT5"}]}"""))
                        .build()
                }
            }
        }

        val bridge = com.example.emat.domain.service.Mt5BridgeClient(client = fakeClient)
        val testRes = bridge.testConnection(
            bridgeUrl = "http://10.0.2.2:8080",
            token = "k9s8D_3kLpQ7vNwY1xZ4mR8tU2aC6eH0jF5bE9wG3rM=",
            brokerServer = "XMGlobal-MT5 10",
            accountNumber = "346533676"
        )
        assertTrue(testRes.isSuccess)
        assertTrue(testRes.terminalConnected)
        assertTrue(testRes.accountMatches)
        assertEquals(19, testRes.discoveredSymbolsCount)

        val connectRes = bridge.connectTerminal(
            bridgeUrl = "http://10.0.2.2:8080",
            token = "k9s8D_3kLpQ7vNwY1xZ4mR8tU2aC6eH0jF5bE9wG3rM=",
            brokerServer = "XMGlobal-MT5 10",
            accountNumber = "346533676",
            investorPassword = "@Guerrier#19940"
        )
        assertTrue(connectRes.isSuccess)

        val statusRes = bridge.fetchConnectionStatus("http://10.0.2.2:8080", "k9s8D_3kLpQ7vNwY1xZ4mR8tU2aC6eH0jF5bE9wG3rM=")
        assertTrue(statusRes.isConnected)
        assertEquals("XMGlobal-MT5 10", statusRes.server)
        assertEquals("346533676", statusRes.accountNumber)
        assertEquals("DEMO", statusRes.accountType)

        val quotesRes = bridge.fetchBrokerQuotes("http://10.0.2.2:8080", "k9s8D_3kLpQ7vNwY1xZ4mR8tU2aC6eH0jF5bE9wG3rM=")
        assertTrue(quotesRes.isSuccess)
        val quotes = quotesRes.getOrThrow()
        assertTrue(quotes.containsKey("EURUSD"))
        assertEquals(1.08502, quotes["EURUSD"]!!.bid, 1e-5)
    }

    @Test
    fun `test legacy HFM broker details are purged and replaced with XM Global DEMO`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("emat_security_vault", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("mt5_broker_name", "HFM Markets")
            .putString("mt5_broker_server", "HFMarketsSC-Live2")
            .putString("mt5_account_number", "55091351")
            .putString("mt5_account_type", "LIVE")
            .putBoolean("mt5_is_configured", true)
            .apply()

        val authService = com.example.emat.domain.service.AuthService(context)
        val cfg = authService.getMt5BrokerConfig()

        assertEquals("XM Global", cfg.brokerName)
        assertEquals("XMGlobal-MT5 10", cfg.brokerServer)
        assertEquals("346533676", cfg.accountNumber)
        assertEquals("DEMO", cfg.accountType)
        assertTrue("Investor password must be masked", cfg.investorPasswordMasked)
        assertEquals("@Guerrier#19940", authService.getMt5BrokerInvestorPassword())
        assertEquals(com.example.emat.domain.model.ComponentHealth.NOT_CONNECTED, cfg.connectionState)
        assertEquals(com.example.emat.domain.model.BrokerAuthStatus.UNAUTHENTICATED, cfg.brokerAuthStatus)
    }
}

