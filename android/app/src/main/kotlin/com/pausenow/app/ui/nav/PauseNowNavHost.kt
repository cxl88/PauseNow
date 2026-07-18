package com.pausenow.app.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pausenow.app.onboarding.OnboardingStore
import com.pausenow.app.ui.PauseNowTheme
import com.pausenow.app.ui.screen.AppPickerScreen
import com.pausenow.app.ui.screen.HomeScreen
import com.pausenow.app.ui.screen.OnboardingScreen
import com.pausenow.app.ui.screen.ReportScreen
import com.pausenow.app.ui.screen.RuleEditScreen
import com.pausenow.app.ui.screen.RulesScreen
import com.pausenow.app.ui.screen.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val RULES = "rules"
    const val RULE_EDIT = "ruleEdit/{ruleId}"
    const val APP_PICKER = "appPicker"
    const val REPORT = "report"
    const val SETTINGS = "settings"

    const val KEY_SELECTED_PACKAGE = "selectedPackage"

    fun ruleEdit(ruleId: String) = "ruleEdit/$ruleId"
}

@Composable
fun PauseNowNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val startDestination = remember {
        if (OnboardingStore(context).isCompleted()) Routes.HOME else Routes.ONBOARDING
    }
    PauseNowTheme {
        NavHost(navController = navController, startDestination = startDestination) {
            composable(Routes.HOME) {
                HomeScreen(
                    onRules = { navController.navigate(Routes.RULES) },
                    onReport = { navController.navigate(Routes.REPORT) },
                    onSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onFinish = {
                    OnboardingStore(context).setCompleted()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                })
            }
            composable(Routes.RULES) {
                RulesScreen(
                    onAdd = { navController.navigate(Routes.ruleEdit("new")) },
                    onEdit = { id -> navController.navigate(Routes.ruleEdit(id)) },
                )
            }
            composable(
                Routes.RULE_EDIT,
                arguments = listOf(navArgument("ruleId") { type = NavType.StringType; defaultValue = "new" }),
            ) { backStackEntry ->
                val ruleId = backStackEntry.arguments?.getString("ruleId") ?: "new"
                val selectedPackage by backStackEntry.savedStateHandle
                    .getStateFlow<String?>(Routes.KEY_SELECTED_PACKAGE, null)
                    .collectAsStateWithLifecycle()
                RuleEditScreen(
                    ruleId = ruleId,
                    selectedPackage = selectedPackage,
                    onConsumeSelected = { backStackEntry.savedStateHandle.remove<String>(Routes.KEY_SELECTED_PACKAGE) },
                    onBack = { navController.popBackStack() },
                    onPickApp = { navController.navigate(Routes.APP_PICKER) },
                )
            }
            composable(Routes.APP_PICKER) {
                AppPickerScreen(onPicked = { pkg ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(Routes.KEY_SELECTED_PACKAGE, pkg)
                    navController.popBackStack()
                })
            }
            composable(Routes.REPORT) { ReportScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
        }
    }
}

@Composable
private fun Placeholder(title: String) {
    Scaffold { padding ->
        Text(title, modifier = Modifier.fillMaxSize().padding(padding))
    }
}
