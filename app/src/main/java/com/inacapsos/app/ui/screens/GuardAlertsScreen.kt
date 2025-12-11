package com.inacapsos.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    fun loadData() {
        scope.launch {
            isLoading = true
            error = null
            try {
                // Obtenemos todos los datos
                val incidentesRaw = repository.getIncidentes()
                incidentes = incidentesRaw
                users = repository.getUsers()
            } catch (e: Exception) {
                error = e.message ?: "Error al cargar datos"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    // SEPARACIÓN DE LISTAS (LÓGICA DE NEGOCIO VISUAL)
    // 1. Alertas SOS: Tienen título "Alerta SOS" (ignorando mayúsculas)
    val sosAlerts = incidentes.filter { it.titulo.contains("Alerta SOS", ignoreCase = true) }

    // 2. Reportes Generales: Todo lo que NO sea SOS
    val generalReports = incidentes.filter { !it.titulo.contains("Alerta SOS", ignoreCase = true) }

    val userMap = users.associateBy { it.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dashboard Supervisor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Gestión de alertas y reportes", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F7FA) // Fondo gris muy suave
    ) { padding ->

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp) // Espacio para no chocar con BottomBar
            ) {
                // SECCIÓN 1: EMERGENCIAS (SOS)
                if (sosAlerts.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Emergencias Activas", icon = Icons.Filled.Warning, color = Color(0xFFD32F2F))
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(sosAlerts) { alerta ->
                                SosCard(alerta, userMap)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // SECCIÓN 2: REPORTES GENERALES
                item {
                    SectionHeader(title = "Bitácora de Reportes", icon = Icons.Filled.ListAlt, color = Color(0xFF1976D2))
                }

                if (generalReports.isEmpty()) {
                    item {
                        Text(
                            "No hay reportes generales pendientes.",
                            modifier = Modifier.padding(16.dp),
                            color = Color.Gray
                        )
                    }
                } else {
                    items(generalReports) { reporte ->
                        ReportCard(reporte, userMap)
                    }
                }
            }
        }
    }
}

// --- COMPONENTES VISUALES ---

@Composable
fun SectionHeader(title: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun SosCard(incidente: IncidenteDto, userMap: Map<String, UserDto>) {
    val context = LocalContext.current
    val userName = incidente.userId?.let { userMap[it]?.nombre } ?: "Desconocido"

    Card(
        modifier = Modifier
            .width(280.dp) // Tarjeta ancha fija para el carrusel
            .height(160.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), // Fondo Rojo Claro
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFD32F2F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("ALERTA SOS", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Hace un momento", color = Color.Gray, fontSize = 12.sp) // Idealmente usar fecha real
                }
            }

            Text(
                "Usuario: $userName activó el botón de pánico.",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Botón de Acción (Mapa)
            Button(
                onClick = {
                    openMap(context, incidente.latitud, incidente.longitud)
                },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver Ubicación", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ReportCard(incidente: IncidenteDto, userMap: Map<String, UserDto>) {
    val context = LocalContext.current
    val userName = incidente.userId?.let { userMap[it]?.nombre } ?: "Anónimo"

    // Determinar icono según título (Lógica simple visual)
    val (icon, color) = when {
        incidente.titulo.contains("Incendio", true) -> Pair(Icons.Filled.LocalFireDepartment, Color(0xFFE64A19))
        incidente.titulo.contains("Robo", true) || incidente.titulo.contains("Acoso", true) -> Pair(Icons.Filled.Security, Color(0xFF5E35B1))
        else -> Pair(Icons.Filled.Info, Color(0xFF0097A7))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Icono Lateral
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Contenido
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = incidente.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Badge de Estado (Visual por ahora)
                    Surface(
                        color = if(incidente.estado == "resuelto") Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = incidente.estado?.uppercase() ?: "PENDIENTE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if(incidente.estado == "resuelto") Color(0xFF2E7D32) else Color(0xFFEF6C00)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = incidente.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Footer de la tarjeta
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Text(text = userName, style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))

                    Spacer(modifier = Modifier.weight(1f))

                    if (incidente.latitud != null && incidente.longitud != null) {
                        Text(
                            text = "Ver mapa",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                openMap(context, incidente.latitud, incidente.longitud)
                            }
                        )
                    }
                }
            }
        }
    }
}

// Función de utilidad para abrir mapa
fun openMap(context: android.content.Context, lat: Double?, lng: Double?) {
    if (lat != null && lng != null) {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Incidente)")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        // Check simple para evitar crash si no hay maps
        try { context.startActivity(intent) } catch (_: Exception) {}
    }
}