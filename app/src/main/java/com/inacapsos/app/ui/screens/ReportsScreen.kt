package com.inacapsos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inacapsos.app.core.AppSession
import com.inacapsos.app.data.remote.dto.FechaDto
import com.inacapsos.app.data.remote.dto.IncidenteDto
import com.inacapsos.app.data.repository.InacapRepository
import com.inacapsos.app.navigation.Screen
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReportsScreen(
    repo: InacapRepository,
    onNavigate: (String) -> Unit
){
    val inacapRed = Color(0xFFCC0000)
    var reports by remember { mutableStateOf<List<IncidenteDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                loading = true
                val allIncidents = repo.getIncidentes()
                reports = allIncidents.filter { it.userId == AppSession.userId }
                    .sortedByDescending { it.timestamp?.seconds ?: 0 }
            } catch (e: Exception) {
                error = "Error al cargar los reportes."
            } finally {
                loading = false
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. HEADER ROJO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(inacapRed)
                    .padding(vertical = 24.dp, horizontal = 20.dp)
            ) {
                Column {
                    Text(
                        "Mis reportes",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Historial de incidentes enviados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // 2. LISTA DE REPORTES (CUERPO)
            // Aquí aplicamos el mismo color de fondo que en SosScreen para que se adapte al tema
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant) // <--- CAMBIO CLAVE
            ) {
                if (loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = inacapRed)
                    }
                } else if (error != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (reports.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(60.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No tienes reportes registrados.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(reports) { incidente ->
                            ReportCardWithIcon(
                                incidente = incidente,
                                onClick = {
                                    onNavigate(Screen.StudentIncidentDetail.createRoute(incidente.id ?: ""))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportCardWithIcon(incidente: IncidenteDto, onClick: () -> Unit) {
    val formattedDate = remember(incidente.timestamp) { formatFechaDto(incidente.timestamp) }
    val title = incidente.titulo ?: "Sin título"

    val (icon, iconColor, bgIconColor) = when {
        title.contains("Médica", true) || title.contains("Salud", true) ->
            Triple(Icons.Filled.MedicalServices, Color(0xFFD32F2F), Color(0xFFFFEBEE))
        title.contains("Incendio", true) || title.contains("Fuego", true) ->
            Triple(Icons.Filled.LocalFireDepartment, Color(0xFFE64A19), Color(0xFFFBE9E7))
        title.contains("Robo", true) || title.contains("Acoso", true) || title.contains("Seguridad", true) ->
            Triple(Icons.Filled.Security, Color(0xFF5E35B1), Color(0xFFEDE7F6))
        title.contains("SOS", true) || title.contains("Pánico", true) ->
            Triple(Icons.Filled.NotificationsActive, Color(0xFFC62828), Color(0xFFFFEBEE))
        else ->
            Triple(Icons.Filled.Info, Color(0xFF0097A7), Color(0xFFE0F7FA))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F6)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(bgIconColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black, // Mantenemos negro para contraste con fondo claro de la tarjeta
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    StatusBadge(status = incidente.estado ?: "pendiente")
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = incidente.descripcion ?: "Sin descripción.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DateRange, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

fun formatFechaDto(fechaDto: FechaDto?): String {
    if (fechaDto == null) return "Fecha desconocida"
    return try {
        val instant = Instant.ofEpochSecond(fechaDto.seconds)
        val zoneId = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault())
        instant.atZone(zoneId).format(formatter)
    } catch (e: Exception) {
        "Fecha inválida"
    }
}

@Composable
fun StatusBadge(status: String) {
    val normalizedStatus = status.lowercase()
    val (backgroundColor, textColor, text) = when (normalizedStatus) {
        "activa", "pendiente" -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pendiente")
        "en_curso", "en curso" -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "En Curso")
        "resuelto", "cerrada" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Resuelto")
        else -> Triple(Color.LightGray.copy(alpha = 0.3f), Color.Gray, status.replaceFirstChar { it.uppercase() })
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}