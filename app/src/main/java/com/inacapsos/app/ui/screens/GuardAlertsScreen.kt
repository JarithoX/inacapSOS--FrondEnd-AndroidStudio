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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inacapsos.app.core.AppSession
import com.inacapsos.app.data.remote.dto.IncidenteDto
import com.inacapsos.app.data.remote.dto.UserDto
import com.inacapsos.app.data.repository.InacapRepository
import com.inacapsos.app.navigation.BottomBar
import com.inacapsos.app.navigation.Screen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Enum para el filtro de fechas
enum class DateFilter(val label: String) {
    TODAY("Hoy"),
    WEEK("Esta Semana"),
    ALL("Todos")
}

@Composable
fun GuardAlertsScreen(
    repository: InacapRepository,
    onNavigate: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var incidentes by remember { mutableStateOf<List<IncidenteDto>>(emptyList()) }
    var users by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    // Variable 'error' eliminada si no se usa en UI, o mantenla para debug
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // --- ESTADOS DE FILTRO Y PAGINACIÓN ---
    var selectedStatusFilter by remember { mutableStateOf("PENDIENTE") }
    var selectedDateFilter by remember { mutableStateOf(DateFilter.TODAY) }
    var currentPage by remember { mutableIntStateOf(1) }
    val itemsPerPage = 5

    val currentUserRole = AppSession.userRole ?: "guardia"

    fun loadData() {
        scope.launch {
            isLoading = true
            errorMsg = null
            try {
                val incidentesRaw = repository.getIncidentes()
                // Ordenamos por fecha descendente
                incidentes = incidentesRaw.sortedByDescending { it.timestamp?.seconds ?: 0 }
                users = repository.getUsers()
            } catch (e: Exception) {
                errorMsg = e.message ?: "Error al cargar datos"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    val userMap = users.associateBy { it.id }

    // 1. SEPARAR SOS: Solo mostrar activas (NO canceladas) en el carrusel de emergencia
    val sosAlerts = incidentes.filter {
        it.titulo.contains("Alerta SOS", ignoreCase = true) &&
                (it.estado?.uppercase() != "CANCELADA")
    }

    // 2. REPORTES GENERALES + SOS CANCELADOS (Para la bitácora)
    val generalReportsRaw = incidentes.filter {
        !it.titulo.contains("Alerta SOS", ignoreCase = true) || (it.estado?.uppercase() == "CANCELADA")
    }

    // LÓGICA DE FILTRADO
    val filteredReports = generalReportsRaw.filter { reporte ->
        // A) Filtro de Estado (Normalizando)
        val rawStatus = reporte.estado?.uppercase() ?: "PENDIENTE"
        val normalizedStatus = when (rawStatus) {
            "ACTIVA", "PENDIENTE" -> "PENDIENTE"
            "EN_CURSO", "EN CURSO" -> "EN_CURSO"
            "RESUELTO", "FINALIZADA" -> "RESUELTO"
            "CANCELADA", "CANCELADO" -> "CANCELADO"
            else -> "PENDIENTE"
        }

        val matchesStatus = normalizedStatus == selectedStatusFilter

        // B) Filtro de Fecha (Usando Calendar para compatibilidad API 24)
        val matchesDate = try {
            val seconds = reporte.timestamp?.seconds ?: 0L
            val reportDate = Date(seconds * 1000L)

            // Fechas para comparar
            val now = Calendar.getInstance()
            val itemCal = Calendar.getInstance().apply { time = reportDate }

            when (selectedDateFilter) {
                DateFilter.TODAY -> {
                    now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                            now.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)
                }
                DateFilter.WEEK -> {
                    // Check simple de última semana (aprox 7 días)
                    val diff = now.timeInMillis - itemCal.timeInMillis
                    val daysDiff = diff / (1000 * 60 * 60 * 24)
                    daysDiff < 7
                }
                DateFilter.ALL -> true
            }
        } catch (e: Exception) {
            true
        }

        matchesStatus && matchesDate
    }

    // 3. LÓGICA DE PAGINACIÓN
    val totalPages = (filteredReports.size + itemsPerPage - 1) / itemsPerPage
    if (currentPage > totalPages && totalPages > 0) currentPage = totalPages
    if (currentPage < 1) currentPage = 1

    val displayedReports = if (filteredReports.isEmpty()) emptyList() else {
        filteredReports
            .drop((currentPage - 1) * itemsPerPage)
            .take(itemsPerPage)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            BottomBar(
                currentRoute = Screen.GuardAlerts.route,
                onNavigate = onNavigate,
                userRole = currentUserRole
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding()),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                // HEADER
                item {
                    DashboardHeader(
                        title = "Dashboard Supervisor",
                        subtitle = "Gestión de alertas y reportes",
                        onRefresh = { loadData() }
                    )
                }

                // SECCIÓN SOS (Solo activas)
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
                                SosCard(
                                    incidente = alerta,
                                    userMap = userMap,
                                    onClick = { onNavigate(Screen.IncidentDetail.createRoute(alerta.id ?: "")) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // SECCIÓN BITÁCORA
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Uso de AutoMirrored para compatibilidad
                            Icon(Icons.AutoMirrored.Filled.ListAlt, null, tint = Color(0xFF1976D2), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Bitácora de Reportes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0D47A1)
                            )
                        }

                        DateFilterDropdown(
                            currentFilter = selectedDateFilter,
                            onFilterSelected = { selectedDateFilter = it; currentPage = 1 }
                        )
                    }

                    // FILTRO DE ESTADO (Con opción "Cancelado")
                    SegmentedStatusControl(
                        selectedStatus = selectedStatusFilter,
                        onStatusSelected = { selectedStatusFilter = it; currentPage = 1 }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // LISTA DE REPORTES
                if (displayedReports.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No hay reportes en esta categoría.", color = Color.Gray)
                        }
                    }
                } else {
                    items(displayedReports) { reporte ->
                        ReportCard(
                            incidente = reporte,
                            userMap = userMap,
                            onClick = { onNavigate(Screen.IncidentDetail.createRoute(reporte.id ?: "")) }
                        )
                    }

                    // PAGINACIÓN
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (currentPage > 1) currentPage-- },
                                enabled = currentPage > 1
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Anterior")
                            }

                            Text(
                                text = "Página $currentPage de ${if (totalPages == 0) 1 else totalPages}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            IconButton(
                                onClick = { if (currentPage < totalPages) currentPage++ },
                                enabled = currentPage < totalPages
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Siguiente")
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENTES VISUALES ---

@Composable
fun SegmentedStatusControl(selectedStatus: String, onStatusSelected: (String) -> Unit) {
    // Agregamos "Cancelado" a las opciones
    val options = listOf(
        "PENDIENTE" to "Pendiente",
        "EN_CURSO" to "En Curso",
        "RESUELTO" to "Resuelto",
        "CANCELADO" to "Cancelado"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label) ->
            val isSelected = selectedStatus == key

            val activeColor = when(key) {
                "PENDIENTE" -> Color(0xFFEF6C00) // Naranja
                "EN_CURSO" -> Color(0xFF1565C0)  // Azul
                "RESUELTO" -> Color(0xFF2E7D32)  // Verde
                "CANCELADO" -> Color(0xFF757575) // Gris
                else -> MaterialTheme.colorScheme.primary
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clickable { onStatusSelected(key) },
                shape = CircleShape,
                color = if (isSelected) activeColor else Color.Transparent,
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                contentColor = if (isSelected) Color.White else Color.Gray
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        // Ajustamos texto pequeño para que quepan 4 botones
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun DateFilterDropdown(currentFilter: DateFilter, onFilterSelected: (DateFilter) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentFilter.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Filled.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DateFilter.values().forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label) },
                    onClick = {
                        onFilterSelected(filter)
                        expanded = false
                    },
                    leadingIcon = if (currentFilter == filter) {
                        { Icon(Icons.Filled.Check, null) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun DashboardHeader(title: String, subtitle: String, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colors = listOf(Color.White, Color(0xFFF5F7FA))))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(36.dp).background(Color.White, CircleShape).shadow(2.dp, CircleShape)
            ) {
                Icon(Icons.Filled.Refresh, "Actualizar", tint = Color(0xFF1976D2), modifier = Modifier.size(20.dp))
            }
        }
    }
}

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
fun SosCard(incidente: IncidenteDto, userMap: Map<String, UserDto>, onClick: () -> Unit) {
    val context = LocalContext.current
    val userName = incidente.userId?.let { userMap[it]?.nombre } ?: "Desconocido"

    // Fecha con SimpleDateFormat (Compatible API 24)
    val formattedTime = remember(incidente.timestamp) {
        try {
            val seconds = incidente.timestamp?.seconds ?: 0L
            if (seconds > 0) {
                val date = Date(seconds * 1000L)
                val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                "Hoy, " + format.format(date)
            } else "Reciente"
        } catch (e: Exception) { "--:--" }
    }

    Card(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).background(Color(0xFFD32F2F), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.NotificationsActive, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("ALERTA SOS", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(formattedTime, color = Color(0xFFB71C1C), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Text(
                "$userName activó el botón de pánico.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF434343),
                maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium
            )
            Button(
                onClick = { openMap(context, incidente.latitud, incidente.longitud) },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver Ubicación", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ReportCard(incidente: IncidenteDto, userMap: Map<String, UserDto>, onClick: () -> Unit) {
    val context = LocalContext.current
    val userName = incidente.userId?.let { userMap[it]?.nombre } ?: "Anónimo"

    val formattedDate = remember(incidente.timestamp) {
        try {
            val seconds = incidente.timestamp?.seconds ?: 0L
            if (seconds > 0) {
                val date = Date(seconds * 1000L)
                val format = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                format.format(date)
            } else "Fecha desc."
        } catch (e: Exception) { "--:--" }
    }

    val (icon, color) = when {
        incidente.titulo.contains("Incendio", true) -> Pair(Icons.Filled.LocalFireDepartment, Color(0xFFE64A19))
        incidente.titulo.contains("Robo", true) || incidente.titulo.contains("Acoso", true) -> Pair(Icons.Filled.Security, Color(0xFF5E35B1))
        incidente.titulo.contains("SOS", true) -> Pair(Icons.Filled.NotificationsActive, Color(0xFFD32F2F))
        else -> Pair(Icons.Filled.Info, Color(0xFF0097A7))
    }

    val statusColor = when(incidente.estado?.uppercase()) {
        "PENDIENTE" -> Color(0xFFEF6C00)
        "EN_CURSO" -> Color(0xFF1565C0)
        "RESUELTO" -> Color(0xFF2E7D32)
        "CANCELADO" -> Color.Gray
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = incidente.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = incidente.estado?.uppercase() ?: "PENDIENTE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = incidente.descripcion, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Person, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Text(text = userName, style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Filled.AccessTime, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Text(text = formattedDate, style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                    Spacer(modifier = Modifier.weight(1f))
                    if (incidente.latitud != null && incidente.longitud != null) {
                        Text(text = "Ver mapa", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { openMap(context, incidente.latitud, incidente.longitud) })
                    }
                }
            }
        }
    }
}

fun openMap(context: android.content.Context, lat: Double?, lng: Double?) {
    if (lat != null && lng != null) {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Incidente)")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        try { context.startActivity(intent) } catch (_: Exception) {}
    }
}