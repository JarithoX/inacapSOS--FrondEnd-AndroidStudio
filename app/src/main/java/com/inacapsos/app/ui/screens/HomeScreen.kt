package com.inacapsos.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inacapsos.app.core.AppSession
import com.inacapsos.app.data.repository.InacapRepository
import com.inacapsos.app.navigation.Screen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security

// Modelo para la UI
data class ReporteResumen(
    val id: String,
    val titulo: String,
    val estado: String,
    val fecha: String
)

@Composable
fun HomeScreen(
    repository: InacapRepository,
    onNavigateToMap: () -> Unit = {},
    onNavigate: (String) -> Unit
) {
    // Datos de Sesión
    val userName = AppSession.userName ?: "Estudiante"
    val userSede = "Sede Renca" // Podrías sacarlo de AppSession si lo guardaras ahí
    val myUserId = AppSession.userId // ID DEL USUARIO LOGUEADO

    val inacapRed = Color(0xFFCC0000)
    val bgColor = Color(0xFFF4F6F8)
    val context = LocalContext.current

    // Estado local
    var reportesDeHoy by remember { mutableStateOf<List<ReporteResumen>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // -----------------------------------------------------------
    // LÓGICA DE CARGA (Igual al Guardia, pero con Filtro Personal)
    // -----------------------------------------------------------
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            // 1. Obtenemos TODOS los incidentes (Misma ruta que el guardia)
            val todosLosReportes = repository.getIncidentes()
            val hoy = LocalDate.now()

            // 2. Filtramos en memoria (Cliente)
            val misReportesHoy = todosLosReportes.mapNotNull { dto ->
                try {
                    // A) Validar que el reporte sea MÍO
                    if (dto.userId != myUserId) return@mapNotNull null

                    // B) Validar que sea de HOY
                    val segundos = dto.timestamp?.seconds ?: 0L
                    val fechaReporte = java.time.Instant.ofEpochSecond(segundos)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()

                    if (fechaReporte.isEqual(hoy)) {
                        // Si pasa ambos filtros, creamos el objeto para la vista
                        ReporteResumen(
                            id = dto.id ?: "",
                            titulo = dto.titulo ?: "Sin título",
                            estado = dto.estado ?: "Pendiente",
                            fecha = fechaReporte.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        )
                    } else {
                        null // Es mío, pero viejo. Descartar.
                    }
                } catch (e: Exception) {
                    Log.e("HomeScreen", "Error procesando reporte: ${dto.id}", e)
                    null
                }
            }
            // Actualizamos la lista visual
            reportesDeHoy = misReportesHoy

        } catch (e: Exception) {
            Log.e("HomeScreen", "Error cargando reportes", e)
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = bgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // HEADER ROJO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        inacapRed,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 30.dp)
            ) {
                Column {
                    Text(
                        text = "Hola, $userName",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = userSede,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // LISTA SCROLLABLE
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 80.dp)
            ) {

                // ACCIONES RÁPIDAS
                item {
                    Text(
                        "Acciones Rápidas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            title = "Nuevo\nReporte",
                            icon = Icons.Default.AddCircle,
                            color = inacapRed,
                            onClick = onNavigateToMap,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionCard(
                            title = "Llamar\nEmergencia",
                            icon = Icons.Default.Call,
                            color = Color(0xFF2E7D32),
                            onClick = {
                                val numero = "133"
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:$numero")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // TÍTULO ACTIVIDAD
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Mis Reportes de Hoy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM")),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // LISTADO DE REPORTES
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = inacapRed)
                        }
                    }
                } else if (reportesDeHoy.isEmpty()) {
                    item { EmptyStateCard() }
                } else {
                    items(reportesDeHoy) { reporte ->
                        ReportItemCard(reporte, onClick = {
                            onNavigate(Screen.StudentIncidentDetail.createRoute(reporte.id))
                        })
                    }
                }
            }
        }
    }
}

// --- COMPONENTES UI (QuickActionCard, EmptyStateCard, ReportItemCard) ---

@Composable
fun QuickActionCard(title: String, icon: ImageVector, color: Color, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.height(100.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.Start) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Sin novedades hoy", fontWeight = FontWeight.SemiBold, color = Color.Gray)
            Text("No has enviado reportes durante el día de hoy.", style = MaterialTheme.typography.bodySmall, color = Color.Gray.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ReportItemCard(reporte: ReporteResumen, onClick: () -> Unit) {
    // 1. Lógica para elegir el ícono y color según el título (estilo guardia)
    val (iconVector, iconColor) = when {
        reporte.titulo.contains("Incendio", true) -> Pair(Icons.Filled.LocalFireDepartment, Color(0xFFE64A19)) // Naranja fuerte
        reporte.titulo.contains("Robo", true) || reporte.titulo.contains("Acoso", true) -> Pair(Icons.Filled.Security, Color(0xFF5E35B1)) // Violeta
        reporte.titulo.contains("SOS", true) || reporte.titulo.contains("Alerta", true) -> Pair(Icons.Filled.NotificationsActive, Color(0xFFD32F2F)) // Rojo
        else -> Pair(Icons.Filled.Info, Color(0xFF0097A7)) // Cian/Turquesa para otros
    }

    // Colores para el estado (mantenemos tu lógica actual)
    val statusBgColor = when(reporte.estado.lowercase()) {
        "pendiente" -> Color(0xFFFF9800).copy(alpha = 0.1f)
        "en proceso", "activa" -> Color(0xFF2196F3).copy(alpha = 0.1f)
        "resuelto" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
        else -> Color.Gray.copy(alpha = 0.1f)
    }
    val statusTextColor = when(reporte.estado.lowercase()) {
        "pendiente" -> Color(0xFFE65100)
        "en proceso", "activa" -> Color(0xFF0D47A1)
        "resuelto" -> Color(0xFF1B5E20)
        else -> Color.DarkGray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp), // espacio entre tarjetas
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp) // Bordes un poco más redondeados
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top // Alineación superior para que el ícono quede bien
        ) {
            // 2. ÍCONO LATERAL (En una caja con fondo suave)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 3. COLUMNA DE CONTENIDO CENTRAL
            Column(modifier = Modifier.weight(1f)) {
                // Fila Superior: Título y Estado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = reporte.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )

                    // Badge de Estado
                    Surface(
                        color = statusBgColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = reporte.estado.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fila Inferior: Fecha con ícono pequeño
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = reporte.fecha,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}