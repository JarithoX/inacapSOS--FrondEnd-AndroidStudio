package com.inacapsos.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import com.inacapsos.app.data.remote.dto.IncidenteDto
import com.inacapsos.app.data.repository.InacapRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentIncidentDetailScreen(
    incidenteId: String,
    repository: InacapRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val peekHeight = 320.dp

    // Configuración: Tiempo de gracia en segundos (2 minutos)
    val gracePeriodSeconds = 120L

    // Configuración Mapa
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
    }

    // Estados de Datos
    var incidente by remember { mutableStateOf<IncidenteDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }

    // Estados de Lógica de Cancelación y Tiempo
    var showQuickCancelConfirm by remember { mutableStateOf(false) }
    var secondsElapsed by remember { mutableLongStateOf(0L) } // Precisión en segundos
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    // Control Mapa
    val incidentLocation = remember(incidente) {
        if (incidente?.latitud != null && incidente?.longitud != null) {
            GeoPoint(incidente!!.latitud, incidente!!.longitud)
        } else null
    }

    // Cargar Datos
    LaunchedEffect(incidenteId) {
        try {
            val todos = repository.getIncidentes()
            incidente = todos.find { it.id == incidenteId }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isLoading = false
        }
    }

    // TEMPORIZADOR PRECISO
    LaunchedEffect(incidente) {
        while (true) {
            incidente?.timestamp?.let { ts ->
                val created = Instant.ofEpochSecond(ts.seconds)
                val now = Instant.now()
                // Calculamos diferencia total en segundos
                secondsElapsed = now.epochSecond - created.epochSecond
            }
            delay(1000) // Actualizar cada segundo
        }
    }

    fun cancelarIncidente(motivo: String? = null) {
        scope.launch {
            isUpdating = true
            val exito = repository.updateIncidenteState(incidenteId, "CANCELADA", motivo)
            if (exito) {
                Toast.makeText(context, "Reporte cancelado correctamente", Toast.LENGTH_SHORT).show()
                incidente = incidente?.copy(estado = "CANCELADA")
                showCancelDialog = false
            } else {
                Toast.makeText(context, "Error al cancelar", Toast.LENGTH_SHORT).show()
            }
            isUpdating = false
        }
    }

    // Helpers para formatear tiempo
    val minutesElapsed = secondsElapsed / 60
    val timeLeft = (gracePeriodSeconds - secondsElapsed).coerceAtLeast(0) // Cuánto falta para los 2 min

    // Formato MM:SS para el temporizador
    val timerText = String.format("%02d:%02d", timeLeft / 60, timeLeft % 60)

    // --- UI ---
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (incidente == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Incidente no encontrado") }
    } else {
        val item = incidente!!

        val estadoActual = item.estado?.uppercase() ?: "PENDIENTE"
        val themeColor = when (estadoActual) {
            "CANCELADA" -> Color.Gray
            "RESUELTO" -> Color(0xFF2E7D32)
            else -> Color(0xFFCC0000)
        }

        BottomSheetScaffold(
            sheetContainerColor = Color.Transparent,
            sheetShadowElevation = 0.dp,
            sheetPeekHeight = peekHeight,
            sheetDragHandle = null,
            sheetContent = {
                Surface(
                    modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().heightIn(min = 400.dp)
                    ) {
                        // Manilla
                        Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp, bottom = 20.dp).width(40.dp).height(4.dp).background(Color.LightGray, CircleShape))

                        // Título y Estado
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)

                                // --- TEMPORIZADOR O TEXTO DE TIEMPO ---
                                if (estadoActual != "CANCELADA" && estadoActual != "RESUELTO" && secondsElapsed < gracePeriodSeconds) {
                                    // MODO CUENTA REGRESIVA
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Timer, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Cancelar en $timerText",
                                            color = Color(0xFFD32F2F), // Rojo alarma
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                } else {
                                    // MODO TEXTO NORMAL
                                    Text("Hace $minutesElapsed minutos", color = Color.Gray, fontSize = 14.sp)
                                }
                            }

                            Surface(color = themeColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                                Text(estadoActual, color = themeColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Descripción", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(item.descripcion, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)

                        Spacer(modifier = Modifier.height(32.dp))

                        // --- ZONA DE ACCIONES ---
                        if (isUpdating) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else if (estadoActual == "CANCELADA") {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE)), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Block, null, tint = Color.Gray)
                                    Spacer(Modifier.width(12.dp))
                                    Text("Este reporte fue cancelado.", color = Color.Gray, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (estadoActual == "RESUELTO") {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF2E7D32))
                                    Spacer(Modifier.width(12.dp))
                                    Text("Caso finalizado con éxito.", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // ES ACTIVA O PENDIENTE -> MOSTRAR OPCIONES DE CANCELAR
                            if (secondsElapsed < gracePeriodSeconds) {
                                // Opción Rápida (< 2 min)
                                Button(
                                    onClick = { showQuickCancelConfirm = true },
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Filled.Close, null)
                                    Spacer(Modifier.width(8.dp))
                                    // Texto del botón también dinámico opcionalmente, pero lo dejamos fijo
                                    Text("CANCELAR AHORA")
                                }
                                Text("Tienes $timerText para cancelar si fue un error.", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(top=8.dp).align(Alignment.CenterHorizontally))
                            } else {
                                // Opción Justificada (> 2 min)
                                Button(
                                    onClick = { showCancelDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("CERRAR REPORTE")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(50.dp))
                    }
                }
            }
        ) {
            // Fondo Mapa (Igual)
            Box(Modifier.fillMaxSize()) {
                if (incidentLocation != null) {
                    AndroidView(factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                            controller.setZoom(18.0)
                            controller.setCenter(incidentLocation)
                            val marker = Marker(this)
                            marker.position = incidentLocation
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            androidx.core.content.ContextCompat.getDrawable(context, com.inacapsos.app.R.drawable.marker_icon_renca_v2)?.let { marker.icon = it }
                            overlays.add(marker)
                        }
                    }, modifier = Modifier.fillMaxSize())
                }
                IconButton(onClick = onNavigateBack, modifier = Modifier.padding(top = 40.dp, start = 16.dp).background(Color.White, CircleShape).shadow(4.dp, CircleShape)) {
                    Icon(Icons.Filled.ArrowBack, "Volver")
                }
            }
        }

        // DIÁLOGO DE MOTIVO
        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Cerrar Reporte") },
                text = {
                    Column {
                        Text("Han pasado más de 2 minutos.\nPor favor, indica el motivo del cierre:")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = cancelReason,
                            onValueChange = { cancelReason = it },
                            placeholder = { Text("Ej: Ya llegó ayuda, Falsa alarma...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { cancelarIncidente(cancelReason) }, enabled = cancelReason.isNotBlank()) {
                        Text("Confirmar Cierre")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) { Text("Volver") }
                }
            )
        }
        if (showQuickCancelConfirm) {
            AlertDialog(
                onDismissRequest = { showQuickCancelConfirm = false },
                title = { Text("¿Cancelar Alerta?  $timerText") },
                text = { Text("Estás a punto de cancelar el llamado de auxilio. ¿Confirmas que fue una falsa alarma?") },
                confirmButton = {
                    Button(
                        onClick = {
                            // Aquí sí ejecutamos la cancelación
                            cancelarIncidente("Toque accidental o falsa alarma")
                            showQuickCancelConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)) // Rojo para confirmar
                    ) {
                        Text("Sí, Cancelar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showQuickCancelConfirm = false }) { Text("No, mantener") }
                }
            )
        }

    }
}