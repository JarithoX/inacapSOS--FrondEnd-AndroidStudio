package com.inacapsos.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.inacapsos.app.core.AppSession

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun BottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    userRole: String
) {

    val defaultItems = listOf(
        BottomNavItem(Screen.Home.route, "Inicio", Icons.Filled.Home),
    )
    val items = mutableListOf<BottomNavItem>()
    items.addAll(defaultItems)

    if (userRole == "ADMIN" || userRole == "admin") {
        items.add(
            BottomNavItem(Screen.Map.route, "Mapa", Icons.Filled.LocationOn)
        )
        items.add(
            BottomNavItem(Screen.Admin.route, "Admin", Icons.Filled.Settings)
        )
    }

    if (userRole == "ESTUDIANTE" || userRole == "estudiante") {
        items.add(
            BottomNavItem(Screen.Sos.route, "SOS", Icons.Filled.Warning)
        )
        items.add(
            BottomNavItem(Screen.Map.route, "Mapa", Icons.Filled.LocationOn)
        )
        items.add(
            BottomNavItem(Screen.Reports.route, "Reportes", Icons.Filled.List)
        )
    }

    if (userRole == "GUARDIA" || userRole == "guardia") {
        items.add(
            BottomNavItem(Screen.GuardAlerts.route, "Alertas", Icons.Filled.AdminPanelSettings)
        )
        items.add(
            BottomNavItem(Screen.Map.route, "Mapa", Icons.Filled.LocationOn)
        )
    }

    items.add(BottomNavItem(Screen.Profile.route, "Perfil", Icons.Filled.Person))

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}