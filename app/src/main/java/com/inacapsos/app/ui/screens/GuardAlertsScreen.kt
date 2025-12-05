package com.inacapsos.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inacapsos.app.data.remote.dto.IncidenteDto
import com.inacapsos.app.data.repository.InacapRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardAlertsScreen(
    repository: InacapRepository
) {
    val scope = rememberCoroutineScope()

    var incidentes by remember { mutableStateOf<List<IncidenteDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedEstado by remember { mutableStateOf("TODOS") }

    fun cargarIncidentes() {
        scope.launch {
            isLoading = true
            error = null
            try {
                incidentes = repository.getIncidentes()
            } catch (e: Exception) {
                error = e.message ?: "No se pudieron cargar las alertas."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        cargarIncidentes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Alertas - Guardia") },
                actions = {
                    IconButton(onClick = { cargarIncidentes() }) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // FILTROS DE ESTADO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EstadoChip("TODOS", selectedEstado) { selectedEstado = it }
                EstadoChip("PENDIENTE", selectedEstado) { selectedEstado = it }
                EstadoChip("EN_CURSO", selectedEstado) { selectedEstado = it }
                EstadoChip("RESUELTO", selectedEstado) { selectedEstado = it }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                    error != null -> {
                        Text(
                            text = error ?: "Error desconocido",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    incidentes.isEmpty() -> {
                        Text(
                            text = "No hay alertas registradas.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        val listaFiltrada = incidentes.filter { incidente ->
                            selectedEstado == "TODOS" ||
                                    incidente.estado.equals(selectedEstado, ignoreCase = true)
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(listaFiltrada) { incidente ->
                                IncidenteCard(incidente)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EstadoChip(
    texto: String,
    seleccionado: String,
    onClick: (String) -> Unit
) {
    FilterChip(
        selected = seleccionado == texto,
        onClick = { onClick(texto) },
        label = { Text(texto) }
    )
}

@Composable
private fun IncidenteCard(
    incidente: IncidenteDto
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = incidente.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = incidente.descripcion)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Estado: ${incidente.estado}")
            incidente.userId?.let { Text("Usuario: $it") }
            incidente.latitud?.let { lat ->
                incidente.longitud?.let { lng ->
                    Text("Ubicación: $lat, $lng")
                }
            }
        }
    }
}
