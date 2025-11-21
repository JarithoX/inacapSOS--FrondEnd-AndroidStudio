package com.inacapsos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onNavigateToCreateGuard: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Panel de Administrador") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Gestión de Cuentas",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
            )

            Button(
                onClick = onNavigateToCreateGuard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear Cuenta de Guardia")
            }

            // Aquí se podría agregar más adelante una lista de usuarios
        }
    }
}
