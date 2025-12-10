package com.inacapsos.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.inacapsos.app.core.AppSession

@Composable
fun GuardBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = mutableListOf<BottomNavItem>()

    if (AppSession.userRole == "GUARD") {
        items.add(BottomNavItem(Screen.GuardAlerts.route, "Alertas", Icons.Filled.List))
    }
    items.add(BottomNavItem(Screen.Map.route, "Mapa", Icons.Filled.Map))
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
