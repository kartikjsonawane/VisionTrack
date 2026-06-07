package com.visiontrack.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.visiontrack.presentation.analytics.AnalyticsDashboardScreen
import com.visiontrack.presentation.auth.AuthViewModel
import com.visiontrack.presentation.auth.LoginScreen
import com.visiontrack.presentation.auth.RegisterScreen
import com.visiontrack.presentation.detection.LiveDetectionScreen
import com.visiontrack.presentation.history.DetectionHistoryScreen
import com.visiontrack.presentation.home.HomeScreen
import com.visiontrack.presentation.profile.ProfileScreen
import com.visiontrack.presentation.settings.SettingsScreen
import com.visiontrack.presentation.splash.SplashScreen

sealed class Screen(val route: String) {
    object Splash      : Screen("splash")
    object Login       : Screen("login")
    object Register    : Screen("register")
    object Home        : Screen("home")
    object Detection   : Screen("detection/{userId}") {
        fun buildRoute(uid: String) = "detection/$uid"
    }
    object History     : Screen("history/{userId}") {
        fun buildRoute(uid: String) = "history/$uid"
    }
    object Analytics   : Screen("analytics/{userId}") {
        fun buildRoute(uid: String) = "analytics/$uid"
    }
    object Profile     : Screen("profile/{userId}") {
        fun buildRoute(uid: String) = "profile/$uid"
    }
    object Settings    : Screen("settings")
}

@Composable
fun VisionTrackNavGraph(navController: NavHostController = rememberNavController()) {
    // authViewModel is passed to SplashScreen only — NOT collected here so NavGraph
    // does not recompose on every Firebase Auth token refresh.
    val authViewModel: AuthViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = { uid ->
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { uid ->
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { uid ->
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDetection = { uid -> navController.navigate(Screen.Detection.buildRoute(uid)) },
                onNavigateToHistory   = { uid -> navController.navigate(Screen.History.buildRoute(uid)) },
                onNavigateToAnalytics = { uid -> navController.navigate(Screen.Analytics.buildRoute(uid)) },
                onNavigateToProfile   = { uid -> navController.navigate(Screen.Profile.buildRoute(uid)) },
                onNavigateToSettings  = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Detection.route) { backStack ->
            val uid = backStack.arguments?.getString("userId") ?: ""
            LiveDetectionScreen(userId = uid, onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.History.route) { backStack ->
            val uid = backStack.arguments?.getString("userId") ?: ""
            DetectionHistoryScreen(userId = uid, onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Analytics.route) { backStack ->
            val uid = backStack.arguments?.getString("userId") ?: ""
            AnalyticsDashboardScreen(userId = uid, onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Profile.route) { backStack ->
            val uid = backStack.arguments?.getString("userId") ?: ""
            ProfileScreen(
                userId = uid,
                onNavigateBack  = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
