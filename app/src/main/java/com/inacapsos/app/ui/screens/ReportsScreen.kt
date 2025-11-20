package com.inacapsos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inacapsos.app.core.AppSession
// CAMBIO: Importar IncidenteDto en lugar de ReportDto
import com.inacapsos.app.data.remote.dto.IncidenteDto
import com.inacapsos.app.data.repository.InacapRepositoryImpl
import kotlinx.coroutines.launch

@Composable
fun ReportsScreen() {
    val inacapRed = Color(0xFFCC0000)
    val repo = remember { InacapRepositoryImpl() }

    // CAMBIO: El estado ahora almacena una lista de IncidenteDto
    var reports by remember { mutableStateOf<List<IncidenteDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // CAMBIO: LaunchedEffect ahora llama a la API real, maneja errores y filtra los resultados
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                // 1. Obtener TODOS los incidentes desde la API
                val allIncidents = repo.getIncidentes()
                // 2. Filtrar para mostrar solo los reportes del usuario actual
                reports = allIncidents.filter { it.userId == AppSession.userId }
            } catch (e: Exception) {
                error = "Error al cargar los reportes."
            } finally {
                loading = false
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column {
            // 🔴 HEADER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(inacapRed)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        "Mis reportes",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Estado de los reportes enviados",
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // 🩶 FONDO GRIS CLARO
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                if (loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (error != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error!!)
                    }
                } else if (reports.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tienes reportes registrados.")
                    }
                } else {
                    // CAMBIO: Se pasa la lista de 'IncidenteDto' filtrada al LazyColumn
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(reports) { incidente ->
                            ReportCardItem(incidente)
                        }
                    }
                }
            }
        }
    }
}

// CAMBIO: El Composable ahora recibe un IncidenteDto y muestra sus datos
@Composable
fun ReportCardItem(incidente: IncidenteDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // NUEVO: Fila para mostrar el título y el estado del incidente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Se usa 'titulo' del IncidenteDto
                Text(incidente.titulo ?: "Sin título", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // NUEVO: Indicador visual para el estado del reporte
                StatusBadge(status = incidente.estado ?: "desconocido")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Se usa 'descripcion' del IncidenteDto
            Text(
                incidente.descripcion ?: "Sin descripción.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
    }
}

// NUEVO: Composable para el indicador de estado, hace el código más limpio
@Composable
fun StatusBadge(status: String) {
    val (color, textColor) = when (status.lowercase()) {
        "activa" -> Color(0xFFE53935) to Color.White // Rojo
        "atendida" -> Color(0xFFFFB300) to Color.Black // Ámbar
        "cerrada" -> Color(0xFF43A047) to Color.White // Verde
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) to MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .background(color = color, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
