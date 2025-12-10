package com.inacapsos.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inacapsos.app.data.model.Guardia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onNavigateToCreateGuard: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val guardias by viewModel.listaGuardias.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 1. ESTADO NUEVO: Aquí guardamos al guardia que "quizás" vamos a borrar
    var guardiaAEliminar by remember { mutableStateOf<Guardia?>(null) }

    // 2. LA ALERTA: Solo se muestra si "guardiaAEliminar" NO es nulo
    if (guardiaAEliminar != null) {
        AlertDialog(
            onDismissRequest = { guardiaAEliminar = null }, // Si tocan fuera, se cierra
            title = { Text(text = "Confirmar eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar al guardia '${guardiaAEliminar?.nombre}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // AQUÍ SI BORRAMOS DE VERDAD
                        guardiaAEliminar?.let { viewModel.eliminarGuardia(it.id) }
                        guardiaAEliminar = null // Cerramos la alerta
                    }
                ) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { guardiaAEliminar = null } // Solo cerramos la alerta
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

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
                style = MaterialTheme.typography.headlineMedium
            )

            Button(
                onClick = onNavigateToCreateGuard,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
            ) {
                Text("Crear Cuenta de Guardia")
            }

            HorizontalDivider()

            Text(
                text = "Lista de Guardias Activos",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            if (isLoading) {
                CircularProgressIndicator()
            } else if (guardias.isEmpty()) {
                Text(
                    text = "No hay guardias registrados.",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 20.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(guardias) { guardia ->
                        GuardiaItem(
                            guardia = guardia,
                            // 3. CAMBIO EN EL CLICK: Ya no borra directo, solo "marca" al guardia para eliminar
                            onDeleteClick = { guardiaAEliminar = guardia }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GuardiaItem(
    guardia: Guardia,
    onDeleteClick: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = guardia.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = guardia.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar guardia",
                    tint = Color.Red
                )
            }
        }
    }
}