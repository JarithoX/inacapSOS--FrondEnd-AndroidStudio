package com.inacapsos.app.ui.screens

import android.content.Intent
import androidx.core.content.ContextCompat
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import com.inacapsos.app.data.remote.dto.IncidenteDto
import com.inacapsos.app.data.remote.dto.UserDto
import com.inacapsos.app.data.repository.InacapRepository
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    incidenteId: String,
    repository: InacapRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val peekHeight = 150.dp

    // Configuración OSMDroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = "com.inacapsos.app"
    }

    // Datos
    var incidente by remember { mutableStateOf<IncidenteDto?>(null) }
    var reportero by remember { mutableStateOf<UserDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }

    // Control del Mapa
    var mapController by remember { mutableStateOf<org.osmdroid.api.IMapController?>(null) }
    val incidentLocation = remember(incidente) {
        if (incidente?.latitud != null && incidente?.longitud != null) {
            GeoPoint(incidente!!.latitud, incidente!!.longitud)
        } else null
    }

    // Estado del BottomSheet
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    // Cargar datos
    LaunchedEffect(incidenteId) {
        try {
            val todos = repository.getIncidentes()
            val encontrado = todos.find { it.id == incidenteId }
            if (encontrado != null) {
                incidente = encontrado
                val usuarios = repository.getUsers()
                reportero = usuarios.find { it.id == encontrado.userId }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isLoading = false
        }
    }

    fun cambiarEstado(nuevoEstado: String) {
        scope.launch {
            isUpdating = true
            val exito = repository.updateIncidenteState(incidenteId, nuevoEstado, null)
            if (exito) {
                Toast.makeText(context, "Estado actualizado", Toast.LENGTH_SHORT).show()
                incidente = incidente?.copy(estado = nuevoEstado)
            } else {
                Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
            }
            isUpdating = false
        }
    }

    fun launchIntent(intent: Intent) {
        try { context.startActivity(intent) } catch (e: Exception) {
            Toast.makeText(context, "No hay app disponible", Toast.LENGTH_SHORT).show()
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (incidente == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Incidente no encontrado") }
    } else {
        val item = incidente!!

        // 1. CORRECCIÓN DE ESTADOS: Agregamos "CANCELADA" para que no caiga en el "else -> PENDIENTE"
        val estadoNormalizado = when (item.estado?.uppercase()) {
            "ACTIVA", "PENDIENTE", null -> "PENDIENTE"
            "EN_CURSO", "EN CURSO" -> "EN_CURSO"
            "RESUELTO" -> "RESUELTO"
            "CANCELADA", "CANCELADO" -> "CANCELADA" // <--- NUEVO
            else -> "PENDIENTE"
        }

        // 2. COLOR GRIS PARA CANCELADOS
        val themeColor = when(estadoNormalizado) {
            "PENDIENTE" -> Color(0xFFEF6C00)
            "EN_CURSO" -> Color(0xFF1565C0)
            "RESUELTO" -> Color(0xFF2E7D32)
            "CANCELADA" -> Color.Gray // <--- NUEVO
            else -> Color.Gray
        }

        // --- ESTRUCTURA PRINCIPAL ---
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetContainerColor = Color.Transparent,
            sheetShadowElevation = 0.dp,
            sheetPeekHeight = peekHeight,
            sheetDragHandle = null,

            // --- HOJA DESLIZANTE (SOLO INFO USUARIO) ---
            sheetContent = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth()
                            .heightIn(min = 400.dp)
                    ) {
                        // Manilla visual
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 12.dp, bottom = 20.dp)
                                .width(40.dp)
                                .height(4.dp)
                                .background(Color.LightGray, CircleShape)
                        )

                        // Info Usuario
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val inicial = reportero?.nombre?.firstOrNull()?.toString() ?: "?"
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFFF5F5F5), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(inicial, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(reportero?.nombre ?: "Usuario Anónimo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(reportero?.email ?: "Sin contacto", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ActionChip(Icons.Filled.Call, "Llamar") { launchIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:"))) }
                                    ActionChip(Icons.Filled.Email, "Email") { reportero?.email?.let { launchIntent(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$it"))) } }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

                        // Info Incidente
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                                val fechaStr = remember(item.timestamp) {
                                    try {
                                        val seconds = item.timestamp?.seconds ?: 0
                                        if (seconds > 0) {
                                            val instant = Instant.ofEpochSecond(seconds)
                                            DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.getDefault())
                                                .format(instant.atZone(ZoneId.systemDefault()))
                                        } else "Hace un momento"
                                    } catch (e: Exception) { "Desconocido" }
                                }
                                Text(fechaStr, color = Color.Gray, fontSize = 14.sp)
                            }
                            Surface(
                                color = themeColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = estadoNormalizado,
                                    color = themeColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Descripción", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(item.descripcion, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)

                        if (estadoNormalizado == "CANCELADA") {
                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Motivo de Cancelación", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                            // Lógica del texto por defecto
                            val motivoTexto = if (!item.motivo_cancelacion.isNullOrBlank()) item.motivo_cancelacion else "Toque accidental o falsa alarma"

                            Text(motivoTexto, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Botones Gestión
                        if (isUpdating) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            when (estadoNormalizado) {
                                "PENDIENTE" -> {
                                    PrimaryButton("TOMAR CASO", Icons.Filled.PanTool, themeColor) { cambiarEstado("EN_CURSO") }
                                    InfoText("Se notificará al usuario.")
                                }
                                "EN_CURSO" -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedButton(
                                            onClick = { },
                                            modifier = Modifier.weight(1f).height(54.dp),
                                            shape = RoundedCornerShape(14.dp)
                                        ) { Text("Pedir Apoyo") }
                                        Button(
                                            onClick = { cambiarEstado("RESUELTO") },
                                            modifier = Modifier.weight(1f).height(54.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            shape = RoundedCornerShape(14.dp)
                                        ) { Icon(Icons.Filled.Check, null); Spacer(Modifier.width(8.dp)); Text("FINALIZAR") }
                                    }
                                }
                                "RESUELTO" -> {
                                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), modifier = Modifier.fillMaxWidth()) {
                                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Verified, null, tint = Color(0xFF2E7D32)); Spacer(Modifier.width(12.dp))
                                            Text("Caso Finalizado", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                "CANCELADA" -> {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.Block, null, tint = Color.Gray)
                                            Spacer(Modifier.width(12.dp))
                                            Text("Reporte Cancelado", color = Color.Gray, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(50.dp))
                    }
                }
            }
        ) { paddingValues ->

            // --- CAPA DE FONDO: MAPA ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (incidentLocation != null) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                                controller.setZoom(18.0)
                                controller.setCenter(incidentLocation)
                                mapController = controller

                                val marker = Marker(this)
                                marker.position = incidentLocation
                                marker.title = item.titulo
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                ContextCompat.getDrawable(context, com.inacapsos.app.R.drawable.marker_icon_renca_v2)?.let { customIcon ->
                                    marker.icon = customIcon
                                }
                                overlays.add(marker)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Color.LightGray), contentAlignment = Alignment.Center) {
                        Text("Sin GPS", color = Color.Gray)
                    }
                }

                // CONTROLES DE ZOOM (Izquierda)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp, bottom = 60.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallFloatingActionButton(onClick = { mapController?.zoomIn() }, containerColor = Color.White) { Icon(Icons.Filled.Add, "Zoom In") }
                    SmallFloatingActionButton(onClick = { mapController?.zoomOut() }, containerColor = Color.White) { Icon(Icons.Filled.Remove, "Zoom Out") }
                }

                // BOTONES FLOTANTES (Recentrar / Ir al lugar)
                // Ahora viven en el mapa, pero con un padding inferior para no quedar tapados por la hoja
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp)
                        .padding(bottom = peekHeight + 16.dp), // Se mueven dinámicamente sobre la hoja inicial
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    SmallFloatingActionButton(
                        onClick = { incidentLocation?.let { mapController?.animateTo(it) } },
                        containerColor = Color.White
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Recentrar", tint = Color.Gray)
                    }

                    ExtendedFloatingActionButton(
                        onClick = {
                            val uri = Uri.parse("google.navigation:q=${item.latitud},${item.longitud}")
                            launchIntent(Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps"))
                        },
                        containerColor = themeColor,
                        contentColor = Color.White,
                        icon = { Icon(Icons.Filled.Navigation, null) },
                        text = { Text("Ir al lugar") }
                    )
                }

                // Botón Volver
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .padding(top = 40.dp, start = 16.dp)
                        .align(Alignment.TopStart)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                        .shadow(4.dp, CircleShape)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                }
            }
        }
    }
}

// --- Componentes Auxiliares ---
@Composable
fun ActionChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color(0xFFF5F5F5),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.DarkGray)
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
        }
    }
}

@Composable
fun PrimaryButton(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Icon(icon, null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}