package com.inacapsos.app.navigation

import androidx.compose.material.icons.Icons
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

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun BottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    userRole: String // Este valor debe ser pasado desde la capa superior (ej. la Activity o un ViewModel)
) {
    val defaultItems = listOf(
        BottomNavItem(Screen.Home.route, "Inicio", Icons.Filled.Home),
        BottomNavItem(Screen.Sos.route, "SOS", Icons.Filled.Warning),
        BottomNavItem(Screen.Map.route, "Mapa", Icons.Filled.LocationOn),
        BottomNavItem(Screen.Reports.route, "Reportes", Icons.Filled.List),
        BottomNavItem(Screen.Profile.route, "Perfil", Icons.Filled.Person)
    )

    val items = mutableListOf<BottomNavItem>()
    items.addAll(defaultItems)

    if (userRole == "ADMIN" || userRole == "admin") {
        items.add(
            BottomNavItem(Screen.Admin.route, "Admin", Icons.Filled.Settings)
        )
    }

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
