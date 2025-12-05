package com.inacapsos.app.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")
    object Sos : Screen("sos")
    object Map : Screen("map")
    object Reports : Screen("reports")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile_screen")
    object Register : Screen("register_screen")
    object Admin : Screen("admin")
    object CreateGuard : Screen("create_guard")

    object GuardAlerts : Screen("guard_alerts")


}
