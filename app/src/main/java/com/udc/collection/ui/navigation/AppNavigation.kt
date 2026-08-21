package com.udc.collection.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.udc.collection.ui.screen.catalogue.TestCatalogueScreen
import com.udc.collection.ui.screen.history.PatientHistoryScreen
import com.udc.collection.ui.screen.home.HomeScreen
import com.udc.collection.ui.screen.onboarding.OnboardingScreen
import com.udc.collection.ui.screen.onboarding.OnboardingViewModel
import com.udc.collection.ui.screen.patient.PatientDetailScreen
import com.udc.collection.ui.screen.patient.PatientWizardScreen
import com.udc.collection.ui.screen.patient.PatientWizardViewModel
import com.udc.collection.ui.screen.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object WizardGraph : Screen("wizard_graph")
    object Wizard : Screen("wizard")
    object PatientHistory : Screen("customer_history")
    object PatientDetail : Screen("customer_detail/{customerId}") { fun createRoute(id: Long) = "customer_detail/$id" }
    object TestCatalogue : Screen("service_catalogue")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val onboardingVm: OnboardingViewModel = hiltViewModel()
    val onboardingDone by onboardingVm.onboardingDone.collectAsState(initial = null)
    val startDestination = when (onboardingDone) { null -> return; false -> Screen.Onboarding.route; else -> Screen.Home.route }
    NavHost(navController, startDestination = startDestination) {
        composable(Screen.Onboarding.route) { OnboardingScreen { navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } } } }
        composable(Screen.Home.route) {
            HomeScreen(
                onNewPatient = { navController.navigate(Screen.WizardGraph.route) },
                onPatientHistory = { navController.navigate(Screen.PatientHistory.route) },
                onTestCatalogue = { navController.navigate(Screen.TestCatalogue.route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onPatientClick = { id -> navController.navigate(Screen.PatientDetail.createRoute(id)) }
            )
        }
        navigation(startDestination = Screen.Wizard.route, route = Screen.WizardGraph.route) {
            composable(Screen.Wizard.route) { entry ->
                val parentEntry = rememberParentEntry(entry, Screen.WizardGraph.route, navController)
                val vm: PatientWizardViewModel = hiltViewModel(parentEntry)
                PatientWizardScreen(onBack = { navController.popBackStack() }, onSaved = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } }, viewModel = vm)
            }
        }
        composable(Screen.PatientHistory.route) { PatientHistoryScreen(onBack = { navController.popBackStack() }, onPatientClick = { id -> navController.navigate(Screen.PatientDetail.createRoute(id)) }) }
        composable(Screen.PatientDetail.route, arguments = listOf(navArgument("customerId") { type = NavType.LongType })) { entry ->
            val customerId = entry.arguments?.getLong("customerId") ?: return@composable
            PatientDetailScreen(customerId, onBack = { navController.popBackStack() })
        }
        composable(Screen.TestCatalogue.route) { TestCatalogueScreen { navController.popBackStack() } }
        composable(Screen.Settings.route) { SettingsScreen { navController.popBackStack() } }
    }
}

@Composable
private fun rememberParentEntry(entry: NavBackStackEntry, parentRoute: String, navController: NavController): NavBackStackEntry = remember(entry) { navController.getBackStackEntry(parentRoute) }
