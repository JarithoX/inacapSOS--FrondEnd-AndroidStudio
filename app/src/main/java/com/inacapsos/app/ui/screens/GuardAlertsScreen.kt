package com.inacapsos.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inacapsos.app.data.remote.dto.IncidenteDto
import com.inacapsos.app.data.remote.dto.UserDto
import com.inacapsos.app.data.repository.InacapRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardAlertsScreen(
    repository: InacapRepository
) {
    val scope = rememberCoroutineScope()

    var incidentes by remember { mutableStateOf<List<IncidenteDto>>(emptyList()) }
    var users by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedEstado by remember { mutableStateOf("TODOS") }

    fun loadData() {
        scope.launch {
            isLoading = true
            error = null
            try {
                incidentes = repository.getIncidentes()
                users = repository.getUsers()
            } catch (e: Exception) {
                error = e.message ?: "No se pudieron cargar las alertas."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Alertas - Guardia") },
                actions = {
                    IconButton(onClick = { loadData() }) {
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

                        val userMap = users.associateBy { it.id }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(listaFiltrada) { incidente ->
                                IncidenteCard(incidente, userMap)
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
    incidente: IncidenteDto,
    userMap: Map<String, UserDto>
) {
    val context = LocalContext.current

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
            incidente.userId?.let {
                val userName = userMap[it]?.nombre ?: "Desconocido"
                Text("Usuario: $userName")
            }
            incidente.latitud?.let { lat ->
                incidente.longitud?.let { lng ->
                    val locationText = "Ubicación: $lat, $lng"
                    Text(
                        text = locationText,
                        modifier = Modifier.clickable {
                            val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Ubicación del incidente)")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)
                        }
                    )
                }
            }
        }
    }
}
