package com.inacapsos.app.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object Sos : Screen("sos")
    object Map : Screen("map")
    object Reports : Screen("reports")
    object Profile : Screen("profile")
}
