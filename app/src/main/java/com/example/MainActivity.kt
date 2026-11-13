package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.example.emat.domain.model.EmatNavDestination
import com.example.emat.domain.service.MarketDataState
import com.example.emat.ui.components.EmatAiContextDialog
import com.example.emat.ui.components.EmatBottomBar
import com.example.emat.ui.components.EmatGlobalSearchDialog
import com.example.emat.ui.components.EmatLeftDrawer
import com.example.emat.ui.components.EmatRecoveryNoticeDialog
import com.example.emat.ui.components.EmatRightUtilityDrawer
import com.example.emat.ui.components.EmatTopBar
import com.example.emat.ui.screens.AuthScreen
import com.example.emat.ui.screens.EmatEvidenceScreen
import com.example.emat.ui.screens.EmatPerformanceScreen
import com.example.emat.ui.screens.EmatReportsScreen
import com.example.emat.ui.screens.EmatRiskScreen
import com.example.emat.ui.screens.EmatSettingsScreen
import com.example.emat.ui.screens.HomeScreen
import com.example.emat.ui.screens.IntelligenceScreen
import com.example.emat.ui.screens.JournalScreen
import com.example.emat.ui.screens.ScannerScreen
import com.example.emat.ui.screens.SplashScreen
import com.example.emat.ui.screens.TradesScreen
import com.example.emat.ui.theme.EmatBackground
import com.example.emat.ui.theme.EmatTheme
import com.example.emat.ui.viewmodel.AppDestination
import com.example.emat.ui.viewmodel.EmatViewModel
import com.example.emat.ui.viewmodel.MainTab
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: EmatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EmatTheme {
                EmatAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun EmatAppRoot(viewModel: EmatViewModel) {
    val destination by viewModel.currentDestination.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val currentNavDestination by viewModel.currentNavDestination.collectAsState()
    val splashState by viewModel.splashState.collectAsState()
    val passwordInput by viewModel.passwordInput.collectAsState()
    val authErrorMessage by viewModel.authErrorMessage.collectAsState()
    val biometricCapability by viewModel.biometricCapability.collectAsState()
    val faceSupported by viewModel.faceSupported.collectAsState()

    val isScanning by viewModel.isScanning.collectAsState()
    val scannerStatus by viewModel.scannerStatus.collectAsState()
    val lastScanTime by viewModel.lastScanTime.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val emergencyStopEngaged by viewModel.emergencyStopEngaged.collectAsState()
    val sastTime by viewModel.sastTime.collectAsState()
    val highImpactNews by viewModel.highImpactNews.collectAsState()

    val instruments by viewModel.instruments.collectAsState()
    val topOpportunities by viewModel.topOpportunities.collectAsState()
    val activeTrade by viewModel.activeTrade.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val systemHealth by viewModel.systemHealth.collectAsState()
    val selectedOpportunity by viewModel.selectedOpportunity.collectAsState()

    // Brick 1 Architecture States
    val currentAccount by viewModel.currentAccount.collectAsState()
    val currentAccountEntitlement by viewModel.currentAccountEntitlement.collectAsState()
    val entitlementRestrictionMessage by viewModel.entitlementRestrictionMessage.collectAsState()
    val selectedLoginAccount by viewModel.selectedLoginAccount.collectAsState()
    val availableAccounts = viewModel.availableAccounts
    val marketDataState by viewModel.marketDataState.collectAsState()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsState()
    val selectedAssetSymbol by viewModel.selectedAssetSymbol.collectAsState()
    val isRightUtilityDrawerOpen by viewModel.isRightUtilityDrawerOpen.collectAsState()
    val isSearchOpen by viewModel.isSearchOpen.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isAiDialogOpen by viewModel.isAiDialogOpen.collectAsState()
    val currentAiAnalysis by viewModel.currentAiAnalysis.collectAsState()
    val oneTimeRecoveryCode by viewModel.oneTimeRecoveryCode.collectAsState()
    val databaseIntegrityStatus by viewModel.databaseIntegrityState.collectAsState()
    val databaseIntegrityPositive by viewModel.databaseIntegrityPositive.collectAsState()
    val aiIntelligenceStatus by viewModel.aiIntelligenceStatus.collectAsState()
    val aiIntelligencePositive by viewModel.aiIntelligencePositive.collectAsState()

    val allTrades by viewModel.allTrades.collectAsState()
    val performanceMetrics by viewModel.performanceMetrics.collectAsState()
    val evidenceRecords by viewModel.evidenceRecords.collectAsState()

    val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Back button handling
    if (destination == AppDestination.MAIN) {
        BackHandler(
            enabled = leftDrawerState.isOpen ||
                isRightUtilityDrawerOpen ||
                isSearchOpen ||
                isAiDialogOpen ||
                currentNavDestination != EmatNavDestination.DASHBOARD
        ) {
            when {
                leftDrawerState.isOpen -> coroutineScope.launch { leftDrawerState.close() }
                isRightUtilityDrawerOpen -> viewModel.closeRightUtilityDrawer()
                isSearchOpen -> viewModel.closeSearch()
                isAiDialogOpen -> viewModel.closeAiAnalysis()
                else -> viewModel.setNavDestination(EmatNavDestination.DASHBOARD)
            }
        }
    }

    Crossfade(targetState = destination, label = "destination_transition") { targetDest ->
        when (targetDest) {
            AppDestination.SPLASH -> {
                SplashScreen(splashState = splashState)
            }

            AppDestination.AUTH -> {
                // AuthScreen is explicitly wrapped in the invariant dark institutional theme
                // ensuring byte-for-byte visual independence from system or user theme selection
                EmatTheme(darkTheme = true) {
                    AuthScreen(
                        passwordInput = passwordInput,
                        errorMessage = authErrorMessage,
                        biometricCapability = biometricCapability,
                        faceSupported = faceSupported,
                        selectedAccount = selectedLoginAccount,
                        availableAccounts = availableAccounts,
                        onAccountSelected = viewModel::selectLoginAccount,
                        onPasswordChanged = viewModel::onPasswordChanged,
                        onLoginPassword = viewModel::loginWithPassword,
                        onLoginBiometric = viewModel::loginWithBiometrics
                    )
                }
            }

            AppDestination.MAIN -> {
                ModalNavigationDrawer(
                    drawerState = leftDrawerState,
                    gesturesEnabled = true,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = Color.Transparent
                        ) {
                            EmatLeftDrawer(
                                currentDestination = currentNavDestination,
                                currentAccount = currentAccount,
                                availableAccounts = availableAccounts,
                                onSelectDestination = { dest ->
                                    coroutineScope.launch { leftDrawerState.close() }
                                    viewModel.setNavDestination(dest)
                                },
                                onSwitchAccount = { account ->
                                    viewModel.switchAccount(account)
                                },
                                onLogout = {
                                    coroutineScope.launch { leftDrawerState.close() }
                                    viewModel.logout()
                                }
                            )
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    // Right-edge swipe detection to open Right Utility Drawer
                                    if (dragAmount < -20f && change.position.x > size.width - 120) {
                                        viewModel.openRightUtilityDrawer()
                                    }
                                }
                            }
                    ) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = EmatBackground,
                            topBar = {
                                EmatTopBar(
                                    isOffline = isOffline,
                                    isEmergencyStopped = emergencyStopEngaged,
                                    sastTime = sastTime,
                                    marketDataState = marketDataState,
                                    onToggleMarketDataState = {
                                        viewModel.setMarketDataState(MarketDataState.NOT_CONNECTED)
                                    },
                                    onEmergencyStopToggle = {
                                        if (emergencyStopEngaged) {
                                            viewModel.resetEmergencyStop()
                                        } else {
                                            viewModel.triggerEmergencyStop()
                                        }
                                    },
                                    onLogout = viewModel::logout,
                                    onOpenLeftDrawer = {
                                        coroutineScope.launch { leftDrawerState.open() }
                                    },
                                    onOpenSearch = viewModel::openSearch,
                                    onOpenRightDrawer = viewModel::openRightUtilityDrawer
                                )
                            },
                            bottomBar = {
                                EmatBottomBar(
                                    currentTab = currentTab,
                                    onTabSelected = viewModel::setTab
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .background(EmatBackground)
                            ) {
                                when (currentNavDestination) {
                                    EmatNavDestination.DASHBOARD -> {
                                        HomeScreen(
                                            isScanning = isScanning,
                                            scannerStatus = scannerStatus,
                                            lastScanTime = lastScanTime,
                                            isOffline = isOffline,
                                            emergencyStopEngaged = emergencyStopEngaged,
                                            sastTime = sastTime,
                                            instruments = instruments,
                                            topOpportunities = topOpportunities,
                                            activeTrade = activeTrade,
                                            systemHealth = systemHealth,
                                            highImpactNews = highImpactNews,
                                            marketDataState = marketDataState,
                                            currentAccount = currentAccount,
                                            currentAccountEntitlement = currentAccountEntitlement,
                                            databaseIntegrityStatus = databaseIntegrityStatus,
                                            databaseIntegrityPositive = databaseIntegrityPositive,
                                            aiIntelligenceStatus = aiIntelligenceStatus,
                                            aiIntelligencePositive = aiIntelligencePositive,
                                            onTriggerScan = viewModel::triggerFullMarketScan,
                                            onEmergencyStopToggle = {
                                                if (emergencyStopEngaged) viewModel.resetEmergencyStop()
                                                else viewModel.triggerEmergencyStop()
                                            },
                                            onOpportunityClick = { opp ->
                                                viewModel.selectOpportunity(opp)
                                                viewModel.setNavDestination(EmatNavDestination.OPPORTUNITIES)
                                            },
                                            onNavigateToScanner = {
                                                viewModel.setNavDestination(EmatNavDestination.OPPORTUNITIES)
                                            },
                                            onStartTrial = viewModel::startTrial,
                                            onRefreshMarketData = viewModel::refreshLiveQuotes
                                        )
                                        val toastContext = androidx.compose.ui.platform.LocalContext.current
                                        androidx.compose.runtime.LaunchedEffect(Unit) {
                                            viewModel.marketDataToastEvent.collect { msg ->
                                                android.widget.Toast.makeText(
                                                    toastContext,
                                                    msg,
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }

                                    EmatNavDestination.MARKET_INTELLIGENCE -> {
                                        IntelligenceScreen(
                                            sastTime = sastTime,
                                            highImpactNews = highImpactNews,
                                            systemHealth = systemHealth
                                        )
                                    }

                                    EmatNavDestination.WATCHLIST, EmatNavDestination.OPPORTUNITIES -> {
                                        ScannerScreen(
                                            isScanning = isScanning,
                                            scannerStatus = scannerStatus,
                                            lastScanTime = lastScanTime,
                                            instruments = instruments,
                                            topOpportunities = topOpportunities,
                                            selectedOpportunity = selectedOpportunity,
                                            onTriggerScan = viewModel::triggerFullMarketScan,
                                            onSelectOpportunity = viewModel::selectOpportunity,
                                            onToggleInstrument = viewModel::toggleInstrumentActive,
                                            onAddInstrument = viewModel::addInstrument,
                                            onRemoveInstrument = viewModel::removeInstrument,
                                            marketDataState = marketDataState
                                        )
                                    }

                                    EmatNavDestination.TRADES -> {
                                        TradesScreen(
                                            activeTrade = activeTrade,
                                            isEmergencyStopped = emergencyStopEngaged,
                                            onEmergencyStopToggle = {
                                                if (emergencyStopEngaged) viewModel.resetEmergencyStop()
                                                else viewModel.triggerEmergencyStop()
                                            },
                                            onOpenDemoTrade = { symbol, direction, entryPrice ->
                                                if (com.example.BuildConfig.DEBUG) {
                                                    viewModel.testOpenDemoTrade(symbol, direction, entryPrice)
                                                }
                                            },
                                            onCloseActiveTrade = viewModel::testCloseActiveTrade
                                        )
                                    }

                                    EmatNavDestination.JOURNAL -> {
                                        JournalScreen(
                                            auditLogs = auditLogs
                                        )
                                    }

                                    EmatNavDestination.EVIDENCE -> {
                                        EmatEvidenceScreen(
                                            evidenceList = evidenceRecords
                                        )
                                    }

                                    EmatNavDestination.PERFORMANCE -> {
                                        EmatPerformanceScreen(
                                            metrics = performanceMetrics,
                                            trades = allTrades
                                        )
                                    }

                                    EmatNavDestination.RISK -> {
                                        EmatRiskScreen(
                                            currentAccount = currentAccount,
                                            activeTrade = activeTrade,
                                            isEmergencyStopped = emergencyStopEngaged,
                                            onEmergencyStopToggle = {
                                                if (emergencyStopEngaged) viewModel.resetEmergencyStop()
                                                else viewModel.triggerEmergencyStop()
                                            }
                                        )
                                    }

                                    EmatNavDestination.REPORTS -> {
                                        EmatReportsScreen(
                                            totalTrades = performanceMetrics.totalTrades,
                                            winRate = performanceMetrics.winRatePercent,
                                            profitFactor = performanceMetrics.profitFactor
                                        )
                                    }

                                    EmatNavDestination.MY_MT5_BROKER -> {
                                        val mt5Config by viewModel.mt5BrokerConfig.collectAsState()
                                        val mt5Diag by viewModel.mt5BridgeTestDiagnostic.collectAsState()
                                        val mt5Quotes by viewModel.mt5ComparisonQuotes.collectAsState()
                                        com.example.emat.ui.screens.Mt5BrokerConnectionScreen(
                                            brokerConfig = mt5Config,
                                            bridgeDiagnostic = mt5Diag,
                                            comparisonQuotes = mt5Quotes,
                                            onSaveConfig = { name, server, acc, type, pwd, url, tok ->
                                                viewModel.saveMt5BrokerConfig(name, server, acc, type, pwd, url, tok)
                                            },
                                            onTestConnection = viewModel::testMt5BridgeConnection,
                                            onConnectMt5 = { name, server, acc, type, pwd, url, tok ->
                                                viewModel.connectMt5Broker(name, server, acc, type, pwd, url, tok)
                                            },
                                            onRunAutomaticWorkflow = viewModel::runAutomaticConnectionWorkflow,
                                            onAutoDetectBridge = viewModel::autoDetectBridge,
                                            onDisconnect = viewModel::disconnectMt5Broker,
                                            onRefresh = viewModel::refreshMt5ComparisonQuotes,
                                            onDeleteConfig = viewModel::deleteMt5BrokerConfig
                                        )
                                    }

                                    else -> {
                                        val marketDataDiagnostic by viewModel.marketDataDiagnostic.collectAsState()
                                        EmatSettingsScreen(
                                            currentDestination = currentNavDestination,
                                            currentAccount = currentAccount,
                                            currentAccountEntitlement = currentAccountEntitlement,
                                            onStartTrial = viewModel::startTrial,
                                            marketDataStatusLabel = marketDataState.label,
                                            marketDataDiagnostic = marketDataDiagnostic,
                                            onConnectLiveMarketData = viewModel::connectLiveMarketData,
                                            onTestMarketDataConnection = viewModel::testMarketDataConnection
                                        )
                                    }
                                }
                            }
                        }

                        // Right Utility Drawer Scrim & Sliding Drawer
                        AnimatedVisibility(
                            visible = isRightUtilityDrawerOpen,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = viewModel::closeRightUtilityDrawer
                                    )
                            )
                        }

                        AnimatedVisibility(
                            visible = isRightUtilityDrawerOpen,
                            enter = slideInHorizontally(initialOffsetX = { it }),
                            exit = slideOutHorizontally(targetOffsetX = { it }),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            EmatRightUtilityDrawer(
                                currentDestination = currentNavDestination,
                                selectedTimeframe = selectedTimeframe,
                                selectedAssetSymbol = selectedAssetSymbol,
                                isScanning = isScanning,
                                isEmergencyStopped = emergencyStopEngaged,
                                onClose = viewModel::closeRightUtilityDrawer,
                                onTriggerScan = {
                                    viewModel.closeRightUtilityDrawer()
                                    viewModel.triggerFullMarketScan()
                                },
                                onSelectTimeframe = viewModel::selectTimeframe,
                                onAskAi = { context ->
                                    viewModel.closeRightUtilityDrawer()
                                    viewModel.openAiAnalysis(context)
                                },
                                onNavigate = { dest ->
                                    viewModel.closeRightUtilityDrawer()
                                    viewModel.setNavDestination(dest)
                                },
                                onEmergencyStopToggle = {
                                    if (emergencyStopEngaged) viewModel.resetEmergencyStop()
                                    else viewModel.triggerEmergencyStop()
                                }
                            )
                        }

                        // Global Search Full-Screen Dialog
                        EmatGlobalSearchDialog(
                            isOpen = isSearchOpen,
                            query = searchQuery,
                            results = searchResults,
                            onQueryChange = viewModel::updateSearchQuery,
                            onClose = viewModel::closeSearch,
                            onSelectResult = { item ->
                                viewModel.closeSearch()
                                viewModel.setNavDestination(item.destination)
                            }
                        )

                        // EMAT AI Decision Support Structured Dialog
                        EmatAiContextDialog(
                            isOpen = isAiDialogOpen,
                            analysisResult = currentAiAnalysis,
                            onClose = viewModel::closeAiAnalysis
                        )

                        // EMAT First-Operator One-Time Recovery Notice Dialog (Dashboard only)
                        EmatRecoveryNoticeDialog(
                            recoveryCode = oneTimeRecoveryCode,
                            onAcknowledge = viewModel::clearOneTimeRecoveryCode
                        )

                        // Entitlement Feature Restriction Alert Dialog
                        if (entitlementRestrictionMessage != null) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = viewModel::clearEntitlementRestrictionMessage,
                                title = {
                                    androidx.compose.material3.Text(
                                        text = "Feature Restricted",
                                        color = com.example.emat.ui.theme.EmatTextWhite,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                },
                                text = {
                                    androidx.compose.material3.Text(
                                        text = entitlementRestrictionMessage ?: "",
                                        color = com.example.emat.ui.theme.EmatTextSubtle
                                    )
                                },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = viewModel::clearEntitlementRestrictionMessage
                                    ) {
                                        androidx.compose.material3.Text("Dismiss", color = com.example.emat.ui.theme.EmatTechCyan)
                                    }
                                },
                                containerColor = com.example.emat.ui.theme.EmatSurfaceCard
                            )
                        }
                    }
                }
            }
        }
    }
}
