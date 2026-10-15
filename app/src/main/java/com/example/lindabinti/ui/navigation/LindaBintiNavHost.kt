package com.example.lindabinti.ui.navigation

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.sos.SosEmergencyCategory
import com.example.lindabinti.sos.SosForegroundService
import com.example.lindabinti.ui.assistant.AiAssistantScreen
import com.example.lindabinti.ui.calculator.CalculatorScreen
import com.example.lindabinti.ui.compass.AiSafetyCompassScreen
import com.example.lindabinti.ui.common.WithActivityResultOwner
import com.example.lindabinti.ui.contacts.TrustedContactsScreen
import com.example.lindabinti.ui.education.EducationScreen
import com.example.lindabinti.ui.emergency.EmergencyAssistanceScreen
import com.example.lindabinti.ui.home.ProtectedHomeScreen
import com.example.lindabinti.ui.ngo.NgoDashboardScreen
import com.example.lindabinti.ui.reporting.ReportingScreen
import com.example.lindabinti.ui.reporting.ReportAiAnalysisScreen
import com.example.lindabinti.ui.risk.RiskAssessmentScreen
import com.example.lindabinti.ui.settings.SettingsScreen
import com.example.lindabinti.ui.setup.SetupWizardScreen
import com.example.lindabinti.ui.support.SurvivorSupportScreen
import com.example.lindabinti.ui.locale.ProvideAppLanguage
import com.example.lindabinti.ui.theme.AppThemeMode
import com.example.lindabinti.ui.theme.LINDABINTITheme
import com.example.lindabinti.ui.ussd.UssdHelpScreen

object Routes {
    const val CALCULATOR = "calculator"
    const val SETUP = "setup"
    const val HOME = "home"
    const val CONTACTS = "contacts"
    const val EMERGENCY = "emergency"
    const val REPORTING = "reporting"
    const val REPORT_ANALYSIS = "report_analysis/{reportId}"
    const val RISK = "risk"
    const val EDUCATION = "education"
    const val SUPPORT = "support"
    const val SETTINGS = "settings"
    const val SAFETY_COMPASS = "safety_compass"
    const val AI = "ai"
    const val NGO = "ngo"
    const val USSD_HELP = "ussd_help"
}

@Composable
fun LindaBintiNavHost() {
    val navController = rememberNavController()
    val app = LindaBintiApp.instance
    val activity = LocalContext.current as ComponentActivity

    var setupComplete by remember { mutableStateOf(app.authRepository.isSetupComplete()) }
    var sosJustTriggered by remember { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val themeMode = when (route) {
        Routes.CALCULATOR, null -> AppThemeMode.Calculator
        else -> AppThemeMode.Protected
    }

    fun goCalculator() {
        sosJustTriggered = false
        app.locationProvider.endIfIdle()
        navController.navigate(Routes.CALCULATOR) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun goHome(fromSos: Boolean) {
        sosJustTriggered = fromSos
        navController.navigate(Routes.HOME) {
            popUpTo(Routes.CALCULATOR) { inclusive = false }
            launchSingleTop = true
        }
    }

    /** Starts SOS SMS first, then opens protected Home. Access path must never call this. */
    fun triggerSos() {
        app.locationProvider.beginSosHold()
        // Prefer direct dispatch (no FGS startup delay). FGS is fallback only.
        try {
            app.enqueueSos(SosEmergencyCategory.GENERAL)
        } catch (_: Exception) {
            try {
                SosForegroundService.start(activity, SosEmergencyCategory.GENERAL)
            } catch (_: Exception) {
            }
        }
        goHome(fromSos = true)
    }

    fun openModule(id: String) {
        val dest = when (id) {
            "emergency" -> Routes.EMERGENCY
            "contacts" -> Routes.CONTACTS
            "reporting" -> Routes.REPORTING
            "risk" -> Routes.RISK
            "education" -> Routes.EDUCATION
            "support" -> Routes.SUPPORT
            "settings" -> Routes.SETTINGS
            "ai" -> Routes.AI
            "safety_compass" -> Routes.SAFETY_COMPASS
            "ngo" -> Routes.NGO
            "ussd_help" -> Routes.USSD_HELP
            else -> return
        }
        navController.navigate(dest)
    }

    // Screenshot blocking is off so documentation captures can be taken on every screen.
    DisposableEffect(route) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { }
    }

    LINDABINTITheme(mode = themeMode) {
        ProvideAppLanguage {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides activity
            ) {
            NavHost(
                navController = navController,
                startDestination = Routes.CALCULATOR
            ) {
            composable(Routes.CALCULATOR) {
                CalculatorScreen(
                    setupComplete = setupComplete,
                    onRequestSetup = {
                        navController.navigate(Routes.SETUP)
                    },
                    onAccessGranted = { goHome(fromSos = false) },
                    onSosTriggered = { triggerSos() },
                    attemptAccessPin = { app.authRepository.attemptAccessPin(it) },
                    verifySosCode = { app.authRepository.verifySosCode(it) }
                )
            }
            composable(Routes.SETUP) {
                WithActivityResultOwner {
                    SetupWizardScreen(
                        onSetupComplete = {
                            setupComplete = true
                            goCalculator()
                        },
                        onCancel = { goCalculator() }
                    )
                }
            }
            composable(Routes.HOME) {
                ProtectedHomeScreen(
                    sosJustTriggered = sosJustTriggered,
                    onQuickHide = { goCalculator() },
                    onOpenModule = { openModule(it) },
                    onResendSos = {
                        SosForegroundService.start(activity)
                    }
                )
            }
            composable(Routes.CONTACTS) {
                WithActivityResultOwner {
                    TrustedContactsScreen(
                        onBack = { navController.popBackStack() },
                        onQuickHide = { goCalculator() }
                    )
                }
            }
            composable(Routes.EMERGENCY) {
                WithActivityResultOwner {
                    EmergencyAssistanceScreen(
                        onBack = { navController.popBackStack() },
                        onQuickHide = { goCalculator() },
                        onSendSos = { category ->
                            app.locationProvider.beginSosHold()
                            SosForegroundService.start(activity, category)
                            sosJustTriggered = true
                            navController.popBackStack(Routes.HOME, inclusive = false)
                        },
                        onOpenContacts = { navController.navigate(Routes.CONTACTS) }
                    )
                }
            }
            composable(Routes.REPORTING) {
                ReportingScreen(
                    onBack = { navController.popBackStack() },
                    onQuickHide = { goCalculator() },
                    onAnalyzeReport = { reportId ->
                        navController.navigate("report_analysis/$reportId")
                    }
                )
            }
            composable(
                route = Routes.REPORT_ANALYSIS,
                arguments = listOf(navArgument("reportId") { type = NavType.LongType })
            ) { entry ->
                val reportId = entry.arguments?.getLong("reportId") ?: return@composable
                ReportAiAnalysisScreen(
                    reportId = reportId,
                    onBack = { navController.popBackStack() },
                    onQuickHide = { goCalculator() },
                    onOpenEmergency = { navController.navigate(Routes.EMERGENCY) },
                    onOpenContacts = { navController.navigate(Routes.CONTACTS) },
                    onOpenSafetyGuidance = { navController.navigate(Routes.RISK) },
                    onOpenEducation = { topicId ->
                        if (topicId.isNullOrBlank()) {
                            navController.navigate(Routes.EDUCATION)
                        } else {
                            navController.navigate("${Routes.EDUCATION}/$topicId")
                        }
                    }
                )
            }
            composable(Routes.RISK) {
                RiskAssessmentScreen(
                    onBack = { navController.popBackStack() },
                    onQuickHide = { goCalculator() },
                    onOpenEmergency = { navController.navigate(Routes.EMERGENCY) },
                    onOpenSupport = { navController.navigate(Routes.SUPPORT) },
                    onOpenEducation = { navController.navigate(Routes.EDUCATION) },
                    onOpenContacts = { navController.navigate(Routes.CONTACTS) }
                )
            }
            composable(Routes.EDUCATION) {
                EducationScreen(
                    onBack = { navController.popBackStack() },
                    onQuickHide = { goCalculator() },
                    onOpenEmergency = { navController.navigate(Routes.EMERGENCY) },
                    onOpenSupport = { navController.navigate(Routes.SUPPORT) },
                    onOpenReporting = { navController.navigate(Routes.REPORTING) }
                )
            }
            composable(
                route = "${Routes.EDUCATION}/{topicId}",
                arguments = listOf(navArgument("topicId") { type = NavType.StringType })
            ) { entry ->
                val topicId = entry.arguments?.getString("topicId")
                EducationScreen(
                    initialTopicId = topicId,
                    onBack = { navController.popBackStack() },
                    onQuickHide = { goCalculator() },
                    onOpenEmergency = { navController.navigate(Routes.EMERGENCY) },
                    onOpenSupport = { navController.navigate(Routes.SUPPORT) },
                    onOpenReporting = { navController.navigate(Routes.REPORTING) }
                )
            }
            composable(Routes.SUPPORT) {
                SurvivorSupportScreen(
                    onBack = { navController.popBackStack() },
                    onQuickHide = { goCalculator() },
                    onOpenEmergency = { navController.navigate(Routes.EMERGENCY) },
                    onOpenContacts = { navController.navigate(Routes.CONTACTS) },
                    onOpenReporting = { navController.navigate(Routes.REPORTING) }
                )
            }
            composable(Routes.SETTINGS) {
                WithActivityResultOwner {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onQuickHide = { goCalculator() },
                        onOpenNgoDashboard = { navController.navigate(Routes.NGO) },
                        onOpenUssdHelp = { navController.navigate(Routes.USSD_HELP) },
                        onDataReset = {
                            setupComplete = false
                            navController.navigate(Routes.SETUP) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
            composable(Routes.SAFETY_COMPASS) {
                AiSafetyCompassScreen(
                    onBack = { navController.popBackStack() },
                    onQuickHide = { goCalculator() },
                    onOpenEmergency = { navController.navigate(Routes.EMERGENCY) },
                    onOpenContacts = { navController.navigate(Routes.CONTACTS) },
                    onOpenSupport = { navController.navigate(Routes.SUPPORT) },
                    onOpenEducation = { topicId ->
                        if (topicId.isNullOrBlank()) {
                            navController.navigate(Routes.EDUCATION)
                        } else {
                            navController.navigate("${Routes.EDUCATION}/$topicId")
                        }
                    },
                    onOpenRiskAssessment = { navController.navigate(Routes.RISK) },
                    onOpenAssistant = { navController.navigate(Routes.AI) }
                )
            }
            composable(Routes.AI) {
                AiAssistantScreen(
                    onBack = { navController.popBackStack() },
                    onQuickHide = { goCalculator() },
                    onOpenAction = { action -> openModule(action) }
                )
            }
            composable(Routes.NGO) {
                NgoDashboardScreen(
                    onBack = { navController.popBackStack() },
                    onQuickHide = { goCalculator() }
                )
            }
            composable(Routes.USSD_HELP) {
                UssdHelpScreen(
                    onBack = { navController.popBackStack() },
                    onQuickHide = { goCalculator() }
                )
            }
            }
            }
        }
    }
}
