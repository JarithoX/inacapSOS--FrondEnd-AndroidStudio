package com.inacapsos.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.inacapsos.app.core.AppSession
import com.inacapsos.app.data.UserRole
import com.inacapsos.app.data.remote.ApiClient
import com.inacapsos.app.data.repository.InacapRepository
import com.inacapsos.app.data.repository.InacapRepositoryImpl
import com.inacapsos.app.ui.screens.*

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Screen.Splash.route

    val repository: InacapRepository = remember {
        InacapRepositoryImpl(ApiClient.api)
    }

    val isGuard = AppSession.userRole == UserRole.GUARD.name

    Scaffold(
        bottomBar = {
            if (!isGuard && currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Sos.route,
                    Screen.Map.route,
                    Screen.Reports.route,
                    Screen.Profile.route,
                    Screen.EditProfile.route,
                    Screen.CreateGuard.route,
                    Screen.Admin.route
                )
            ) {
                BottomBar(
                    currentRoute =
                        currentRoute,
                    userRole = AppSession.userRole ?: "", // Para saber si es admin o no
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(Screen.Splash.route) {
                SplashScreen(
                    onFinished = {
                        val destination = when (AppSession.userRole) {
                            UserRole.ADMIN.name -> Screen.Admin.route
                            UserRole.GUARD.name -> Screen.GuardAlerts.route
                            else -> Screen.Home.route
                        }

                        navController.navigate(destination) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    repository = repository,
                    onLoginSuccess = {
                        val destination = when (AppSession.userRole) {
                            UserRole.ADMIN.name -> Screen.Admin.route
                            UserRole.GUARD.name -> Screen.GuardAlerts.route
                            else -> Screen.Home.route
                        }

                        navController.navigate(destination) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onRegisterClick = { navController.navigate(Screen.Register.route) }
                )
            }

            composable(Screen.Home.route) { HomeScreen() }

            composable(Screen.Admin.route) {
                AdminScreen(
                    onNavigateToCreateGuard = { navController.navigate(Screen.CreateGuard.route) }
                )
            }

            composable(Screen.CreateGuard.route) {
                CreateGuardScreen(
                    repository = repository,
                    onGuardCreated = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.GuardAlerts.route) {
                GuardAlertsScreen(repository)
            }

            composable(Screen.Sos.route) { SosScreen(repository) }

            composable(Screen.Map.route) {
                MapScreen(navController = navController, repository = repository)
            }

            composable(Screen.Reports.route) {
                ReportsScreen(repo = repository)
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        AppSession.clear()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onLogin = { navController.navigate(Screen.Login.route) },
                    onEditProfile = { navController.navigate(Screen.EditProfile.route) }
                )
            }

            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    repository = repository,
                    onRegisterSuccess = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
