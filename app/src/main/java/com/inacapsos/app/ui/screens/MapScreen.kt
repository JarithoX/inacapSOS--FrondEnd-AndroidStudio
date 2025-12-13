package com.inacapsos.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.preference.PreferenceManager
import com.google.android.gms.location.LocationServices
import com.inacapsos.app.R
import com.inacapsos.app.core.AppSession
import com.inacapsos.app.data.remote.dto.ComentarioDto
import com.inacapsos.app.data.remote.dto.IncidenteDto
import com.inacapsos.app.data.remote.dto.UserDto
import com.inacapsos.app.data.repository.InacapRepository
import com.inacapsos.app.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Image

// =======================================================================================
// HELPER: ÍCONOS DE MAPA
// =======================================================================================
private fun getMarkerIconForIncident(
    context: Context,
    incidentTitle: String,
    hasComments: Boolean
): Drawable? {

    val drawableId = if (hasComments) {
        when {
            incidentTitle.contains("Seguridad", true) -> R.drawable.marker_reporte_emergencia_message
            incidentTitle.contains("Médica", true) -> R.drawable.marker_reporte_emergencia_medica_message
            incidentTitle.contains("Incendio", true) -> R.drawable.marker_reporte_incendio_message
            incidentTitle.contains("Robo", true) -> R.drawable.marker_reporte_robo_message
            incidentTitle.contains("Acoso", true) -> R.drawable.marker_reporte_acoso_message
            incidentTitle.contains("SOS", true) -> R.drawable.marker_alerta_sos_message
            incidentTitle.contains("Otro", true) -> R.drawable.marker_reporte_otros_message
            else -> R.drawable.marker_else_message
        }
    } else {
        // VARIANTE NORMAL
        when {
            incidentTitle.contains("Seguridad", true) -> R.drawable.marker_reporte_emergencia
            incidentTitle.contains("Médica", true) -> R.drawable.marker_reporte_emergencia_medica
            incidentTitle.contains("Incendio", true) -> R.drawable.marker_reporte_incendio
            incidentTitle.contains("Robo", true) -> R.drawable.marker_reporte_robo
            incidentTitle.contains("Acoso", true) -> R.drawable.marker_reporte_acoso
            incidentTitle.contains("SOS", true) -> R.drawable.marker_alerta_sos
            incidentTitle.contains("Otro", true) -> R.drawable.marker_reporte_otros
            else -> R.drawable.marker_else
        }
    }
    return ContextCompat.getDrawable(context, drawableId)

    // return getBitmapFromVector(context, drawableId)    // ¡IMPORTANTE! Usar el convertidor para que se vea en el mapa
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavHostController, repository: InacapRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estados UI Globales
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Datos
    var incidentes by remember { mutableStateOf<List<IncidenteDto>>(emptyList()) }
    var userMap by remember { mutableStateOf<Map<String, UserDto>>(emptyMap()) } // Mapa de usuarios para el chat

    // Estados Mapa y Reporte
    var showDialog by remember { mutableStateOf(false) }
    var showMapMenu by remember { mutableStateOf(false) }
    var selectedIncident by remember { mutableStateOf<IncidenteDto?>(null) }
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // Variables Reporte
    var selectedIncidentType by remember { mutableStateOf<IncidentType?>(null) }
    var incidentDescription by remember { mutableStateOf("") }

    // --- ESTADOS PARA EL CHAT (COMENTARIOS) ---
    var showCommentsSheet by remember { mutableStateOf(false) }
    var commentsList by remember { mutableStateOf<List<ComentarioDto>>(emptyList()) }

    // CAMBIO: skipPartiallyExpanded = true para que la hoja se abra completa
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userHeading by remember { mutableFloatStateOf(0f) }

    // Permisos
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) userLocation = GeoPoint(location.latitude, location.longitude)
                }
            }
        } else {
            error = "El permiso de ubicación es necesario."
        }
    }

    // 1. INICIO: Ubicación y Carga de Usuarios/Incidentes
    LaunchedEffect(Unit) {
        // Cargar usuarios para identificar roles en el chat
        launch {
            try {
                val users = repository.getUsers()
                userMap = users.associateBy { it.id }
            } catch (e: Exception) {
                // Error silencioso, usaremos nombres por defecto
            }
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) userLocation = GeoPoint(location.latitude, location.longitude)
            }
        }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometerReading = FloatArray(3)
        val magnetometerReading = FloatArray(3)
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        // FACTOR DE SUAVIZADO (ALPHA)
        val alpha = 0.09f

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    // Aplicamos el filtro a cada eje del acelerómetro
                    accelerometerReading[0] = alpha * event.values[0] + (1 - alpha) * accelerometerReading[0]
                    accelerometerReading[1] = alpha * event.values[1] + (1 - alpha) * accelerometerReading[1]
                    accelerometerReading[2] = alpha * event.values[2] + (1 - alpha) * accelerometerReading[2]

                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    // Aplicamos el filtro a cada eje del magnetómetro
                    magnetometerReading[0] = alpha * event.values[0] + (1 - alpha) * magnetometerReading[0]
                    magnetometerReading[1] = alpha * event.values[1] + (1 - alpha) * magnetometerReading[1]
                    magnetometerReading[2] = alpha * event.values[2] + (1 - alpha) * magnetometerReading[2]
                }
                // Calcular orientación
                SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                // Convertir radianes a grados y ajustar (Azimut)
                val azimuthInRadians = orientationAngles[0]
                val azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()

                // Normalizar a 0-360
                userHeading = (azimuthInDegrees + 360) % 360
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_UI)

        onDispose { sensorManager.unregisterListener(sensorListener) }
    }

    // 2. POLLING: Actualizar mapa cada 20s
    LaunchedEffect(Unit) {
        isLoading = true
        while(true) {
            try {
                isRefreshing = true
                val nuevos = repository.getIncidentes()
                incidentes = nuevos
            } catch (e: Exception) { e.printStackTrace() }
            finally { isLoading = false; isRefreshing = false }
            delay(20000)
        }
    }

    // 3. CARGAR COMENTARIOS: Cuando se abre el sheet
    LaunchedEffect(showCommentsSheet, selectedIncident) {
        if (showCommentsSheet && selectedIncident != null) {
            try {
                commentsList = repository.getComentarios(selectedIncident!!.id!!)
            } catch (e: Exception) { }
        }
    }

    val inacapRencaLocation = GeoPoint(-33.40577356783602, -70.6830789367392)
    val incidentTypes = listOf(
        IncidentType("Emergencia Seguridad", Icons.Default.Security),
        IncidentType("Emergencia Médica", Icons.Default.MedicalServices),
        IncidentType("Incendio", Icons.Default.FireTruck),
        IncidentType("Robo", Icons.Default.Person),
        IncidentType("Acoso", Icons.Default.Report),
        IncidentType("Otro", Icons.Default.AddLocationAlt)
    )

    Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))

    Box(modifier = Modifier.fillMaxSize()) {
        // MAPA
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    controller.setZoom(18.5)
                    controller.setCenter(inacapRencaLocation)
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    setMultiTouchControls(true)
                }.also { mapView = it }
            },
            update = { }
        )

        // BARRA SUPERIOR
        Column(modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp, start = 16.dp, end = 16.dp).fillMaxWidth()) {
            Card(modifier = Modifier.fillMaxWidth().height(50.dp), elevation = CardDefaults.cardElevation(6.dp), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(24.dp)) {
                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, "Buscar", tint = Color.Gray)
                    Spacer(Modifier.width(12.dp))
                    Text("Buscar punto de interes...", style = MaterialTheme.typography.bodyLarge, color = Color.Gray, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.AccountCircle, "Perfil", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                CategoryChip("Reportes", Icons.Default.Info)
                CategoryChip("Puntos de peligro", Icons.Default.Warning)
                CategoryChip("Seguridad", Icons.Default.Security)
            }
        }

        // CONTROLES FLOTANTES
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 120.dp, end = 16.dp)) {
            SmallFloatingActionButton(onClick = { showMapMenu = true }, containerColor = Color.White) { Icon(Icons.Default.Map, "Capas") }
            DropdownMenu(expanded = showMapMenu, onDismissRequest = { showMapMenu = false }) {
                DropdownMenuItem(text = { Text("Mapa Normal") }, onClick = { mapView?.setTileSource(TileSourceFactory.MAPNIK); showMapMenu = false })
                DropdownMenuItem(text = { Text("Mapa Satelital") }, onClick = { mapView?.setTileSource(TileSourceFactory.HIKEBIKEMAP); showMapMenu = false })
            }
        }

        Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallFloatingActionButton(onClick = { mapView?.controller?.zoomIn() }, containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.Add, "Zoom In") }
            SmallFloatingActionButton(onClick = { mapView?.controller?.zoomOut() }, containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.Remove, "Zoom Out") }
            SmallFloatingActionButton(onClick = { mapView?.controller?.animateTo(inacapRencaLocation); mapView?.controller?.setZoom(18.5) }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White) { Icon(Icons.Default.Home, "Centrar") }
            SmallFloatingActionButton(onClick = { userLocation?.let { mapView?.controller?.animateTo(it) } }, containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.MyLocation, "Mi Ubicación") }
        }

        // BOTÓN REPORTAR
        if (selectedIncident == null) {
            FloatingActionButton(
                onClick = {
                    if (AppSession.userId != null) showDialog = true else navController.navigate(Screen.Login.route)
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                containerColor = Color(0xFFCC0000),
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Report, "Reportar Incidente")
            }
        }

        // TARJETA DE INCIDENTE
        AnimatedVisibility(
            visible = selectedIncident != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedIncident?.let { incidente ->
                IncidentMapCard(
                    incidente = incidente,
                    currentUserId = AppSession.userId,
                    onClose = { selectedIncident = null },
                    onManage = { navController.navigate(Screen.StudentIncidentDetail.createRoute(incidente.id ?: "")) },
                    onComments = { showCommentsSheet = true }
                )
            }
        }

        // --- BOTTOM SHEET CHAT (MEJORADO) ---
        if (showCommentsSheet && selectedIncident != null) {
            ModalBottomSheet(
                onDismissRequest = { showCommentsSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                scrimColor = Color.Black.copy(alpha = 0.3f)
            ) {
                CommentsSheetContent(
                    incidente = selectedIncident!!,
                    comments = commentsList,
                    userMap = userMap, // Pasamos el mapa de usuarios
                    repository = repository,
                    onCommentsRefresh = { newList -> commentsList = newList }, // Callback para actualizar lista
                    onCommentSent = { newComment ->
                        // Agregamos localmente para feedback inmediato
                        commentsList = commentsList + newComment
                    }
                )
            }
        }

        // DIÁLOGOS DE REPORTE
        if (showDialog) {
            if (selectedIncidentType == null) {
                IncidentTypeSelectionDialog(incidentTypes, { showDialog = false }, { selectedIncidentType = it })
            } else {
                IncidentDescriptionDialog(
                    selectedIncidentType!!,
                    { showDialog = false; selectedIncidentType = null; incidentDescription = "" },
                    {
                        if (userLocation == null) {
                            error = "Sin ubicación GPS."
                            return@IncidentDescriptionDialog
                        }
                        scope.launch {
                            isLoading = true
                            try {
                                val incidenteData = mapOf(
                                    "titulo" to selectedIncidentType!!.name,
                                    "descripcion" to incidentDescription,
                                    "latitud" to userLocation!!.latitude,
                                    "longitud" to userLocation!!.longitude,
                                    "userId" to (AppSession.userId ?: "anonimo"),
                                    "estado" to "PENDIENTE",
                                    "evidencia_url" to ""
                                )
                                repository.reportIncident(incidenteData)
                                incidentes = repository.getIncidentes()
                                showDialog = false; selectedIncidentType = null; incidentDescription = ""
                            } catch (e: Exception) { error = e.message } finally { isLoading = false }
                        }
                    },
                    incidentDescription, { incidentDescription = it }
                )
            }
        }

        if (isLoading && !isRefreshing) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        error?.let { AlertDialog(onDismissRequest = { error = null }, title = { Text("Error") }, text = { Text(it) }, confirmButton = { Button(onClick = { error = null }) { Text("Aceptar") } }) }
    }

    // MARCADORES MAPA
    LaunchedEffect(incidentes, userLocation, mapView, userHeading) {
        val map = mapView ?: return@LaunchedEffect
        map.overlays.clear()

        val inacapMarker = Marker(map).apply {
            position = inacapRencaLocation
            title = "Sede INACAP Renca"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(context, R.drawable.marker_icon_renca_v2)
            setOnMarkerClickListener { _, _ -> true }
        }
        map.overlays.add(inacapMarker)

        userLocation?.let { loc ->
            val coneMarker = Marker(map).apply {
                position = loc
                // El cono debe estar centrado para rotar bien (0.5, 0.5)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                // Asume que tienes este recurso creado
                icon = ContextCompat.getDrawable(context, R.drawable.marker_vision_cone)
                // Asignamos la rotación de la brújula (negativo porque el mapa rota inverso a veces, prueba + o -)
                rotation = -userHeading
                // Esto hace que ignore el touch, para poder dar click al stickman
                setOnMarkerClickListener { _, _ -> false }
            }
            map.overlays.add(coneMarker)

            val userMarker = Marker(map).apply {
                position = loc
                title = "Tu ubicación"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = ContextCompat.getDrawable(context, R.drawable.marker_position_user)
                setOnMarkerClickListener { _, _ -> true }
            }
            map.overlays.add(userMarker)
        }

        incidentes.forEach { incidente ->
            val estado = incidente.estado?.uppercase() ?: "PENDIENTE"
            if (incidente.latitud != null && incidente.longitud != null &&
                (estado == "PENDIENTE" || estado == "EN_CURSO" || estado == "ACTIVA" || estado == "EN CURSO")) {

                val marker = Marker(map).apply {
                    position = GeoPoint(incidente.latitud, incidente.longitud)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = incidente.titulo
                    icon = getMarkerIconForIncident(
                        context,
                        incidente.titulo ?: "Otro",
                        incidente.tieneComentarios
                    )
                    setOnMarkerClickListener { _, map ->
                        selectedIncident = incidente
                        map.controller.animateTo(position)
                        true
                    }
                }
                map.overlays.add(marker)
            }
        }
        map.invalidate()
    }
}

// --- TARJETA DE MAPA ---
@Composable
fun IncidentMapCard(
    incidente: IncidenteDto,
    currentUserId: String?,
    onClose: () -> Unit,
    onManage: () -> Unit,
    onComments: () -> Unit
) {
    val formattedDate = remember(incidente.timestamp) {
        try {
            val seconds = incidente.timestamp?.seconds ?: 0L
            if (seconds > 0) {
                val date = Date(seconds * 1000L)
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            } else ""
        } catch (e: Exception) { "" }
    }

    // 1. DETERMINAMOS EL RECURSO (DRAWABLE) Y LOS COLORES DE FONDO
    // Nota: Usamos el marker "base" (sin la burbuja de mensaje) para la tarjeta,
    // ya que la tarjeta es grande y clara.
    val (drawableResId, accentColor, bgColor) = when {
        incidente.titulo.contains("Seguridad", true) -> Triple(R.drawable.marker_reporte_emergencia, Color(0xFFD32F2F), Color(0xFFFFEBEE))
        incidente.titulo.contains("Médica", true) -> Triple(R.drawable.marker_reporte_emergencia_medica, Color(0xFFD32F2F), Color(0xFFFFEBEE))
        incidente.titulo.contains("Incendio", true) -> Triple(R.drawable.marker_reporte_incendio, Color(0xFFE64A19), Color(0xFFFBE9E7))
        incidente.titulo.contains("Robo", true) -> Triple(R.drawable.marker_reporte_robo, Color(0xFF5E35B1), Color(0xFFEDE7F6))
        incidente.titulo.contains("Acoso", true) -> Triple(R.drawable.marker_reporte_acoso, Color(0xFF7B1FA2), Color(0xFFF3E5F5))
        incidente.titulo.contains("SOS", true) -> Triple(R.drawable.marker_alerta_sos, Color(0xFFB71C1C), Color(0xFFFFEBEE))
        incidente.titulo.contains("Otro", true) -> Triple(R.drawable.marker_reporte_otros, Color(0xFF0097A7), Color(0xFFE0F7FA))
        // Default / Else
        else -> Triple(R.drawable.marker_else, Color(0xFF607D8B), Color(0xFFECEFF1))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                // CAJA DEL ICONO
                Box(
                    modifier = Modifier
                        .size(56.dp) // Un poco más grande para que luzca el marker
                        .background(bgColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = drawableResId),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp) // Tamaño del marker dentro de la caja
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(incidente.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (formattedDate.isNotEmpty()) {
                        Text("Reportado a las $formattedDate", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Cerrar", tint = Color.Gray) }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = incidente.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(16.dp))

            // Botones
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onComments,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F0F0), contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ChatBubbleOutline, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Comentar")
                }

                if (currentUserId != null && currentUserId == incidente.userId) {
                    Button(
                        onClick = onManage,
                        modifier = Modifier.weight(1f),
                        // Usamos el color de acento definido arriba para el botón
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Gestionar")
                    }
                }
            }
        }
    }
}

// --- CHAT CON AUTO-REFRESH Y USUARIOS REALES ---
@Composable
fun CommentsSheetContent(
    incidente: IncidenteDto,
    comments: List<ComentarioDto>,
    userMap: Map<String, UserDto>,
    repository: InacapRepository,
    onCommentsRefresh: (List<ComentarioDto>) -> Unit,
    onCommentSent: (ComentarioDto) -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // Estado del Scroll
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Temporizador
    var secondsToRefresh by remember { mutableIntStateOf(10) }

    // 1. LÓGICA DE SCROLL INTELIGENTE
    // Detectamos si el usuario está al final de la lista
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf true

            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            // Estamos al fondo si el último item visible es el último de la lista
            lastVisibleItem != null && lastVisibleItem.index >= totalItems - 1
        }
    }

    // Efecto para bajar automáticamente (Auto-Scroll)
    LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) {
            // Solo bajamos si el usuario ya estaba abajo O si es el primer mensaje
            // Esto evita saltos bruscos si estás leyendo el historial arriba
            if (isAtBottom || comments.size == 1) {
                listState.animateScrollToItem(comments.size - 1)
            }
        }
    }

    // 2. CICLO DE ACTUALIZACIÓN
    LaunchedEffect(Unit) {
        while(true) {
            delay(1000)
            secondsToRefresh--
            if (secondsToRefresh <= 0) {
                try {
                    val freshComments = repository.getComentarios(incidente.id!!)
                    onCommentsRefresh(freshComments)
                } catch (_: Exception) {}
                secondsToRefresh = 10
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp, max = 700.dp)
            .padding(16.dp)
            .padding(bottom = 20.dp)
    ) {
        // Cabecera
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Comentarios de la Comunidad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Actualizando en ${secondsToRefresh}s",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Divider()

        // LISTA DE MENSAJES
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (comments.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("Sé el primero en comentar.", color = Color.Gray)
                    }
                }
            } else {
                // Usamos keys para mejorar el rendimiento y evitar saltos visuales al actualizar
                items(
                    items = comments,
                    key = { it.id ?: it.hashCode() }
                ) { comment ->
                    // 1. Lógica de Hora (Formato HH:mm)
                    val formattedTime = remember(comment.timestamp) {
                        try {
                            val seconds = comment.timestamp?.seconds ?: System.currentTimeMillis() / 1000
                            val date = Date(seconds * 1000L)
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                        } catch (e: Exception) {
                            "Ahora" // Si es un mensaje recién enviado optimista
                        }
                    }

                    // Identificación de usuario
                    val user = userMap[comment.userId]
                    val isGuard = user?.rol?.equals("guardia", ignoreCase = true) == true
                    val displayName = user?.nombre ?: comment.nombreUsuario
                    val initial = displayName.firstOrNull()?.toString()?.uppercase() ?: "?"

                    val avatarColor = if (isGuard) Color(0xFF1976D2) else Color(0xFFFFA000)
                    val nameColor = if (isGuard) Color(0xFF0D47A1) else Color.DarkGray
                    val badgeIcon = if (isGuard) Icons.Filled.Security else null

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(avatarColor.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (badgeIcon != null) {
                                Icon(badgeIcon, null, tint = avatarColor, modifier = Modifier.size(18.dp))
                            } else {
                                Text(initial, color = avatarColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // BURBUJA DE CHAT (Color Nuevo y Hora)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0x2CFF0000), RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 12.dp))
                                .padding(10.dp) // Un poco menos de padding para que se vea compacto
                        ) {
                            // Fila Superior: Nombre (Izq) + Hora (Der)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween, // Separa los extremos
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if(isGuard) "$displayName (Guardia)" else displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = nameColor
                                )

                                // HORA A LA DERECHA
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            // Texto del mensaje
                            Text(
                                text = comment.texto,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // INPUT BAR
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                placeholder = { Text("Escribe un reporte...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (newCommentText.isNotBlank()) {
                        val textToSend = newCommentText
                        newCommentText = ""

                        // Enviamos mensaje y FORZAMOS bajar al fondo
                        scope.launch {
                            // 1. Añadir a UI (Optimista)
                            val userId = AppSession.userId ?: "anonimo"
                            val currentUser = userMap[userId]
                            val userName = currentUser?.nombre ?: "Yo"

                            // ID temporal para que la key de LazyColumn no falle
                            val tempId = System.currentTimeMillis().toString()
                            val optimisticComment = ComentarioDto(id = tempId, texto = textToSend, userId = userId, nombreUsuario = userName)

                            onCommentSent(optimisticComment)

                            // 2. Scroll manual al fondo al enviar (Feedback visual de "Enviado")
                            try {
                                // Esperamos un micro-momento para que la lista se actualice con el nuevo item
                                delay(100)
                                listState.animateScrollToItem(comments.size)
                            } catch(_:Exception){}

                            // 3. Enviar a Servidor
                            isSending = true
                            repository.enviarComentario(incidente.id!!, textToSend, userId, userName)
                            isSending = false
                        }
                    }
                },
                enabled = newCommentText.isNotBlank() && !isSending,
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, "Enviar", tint = Color.White)
                }
            }
        }
    }
}

// ... (Resto de diálogos igual)
@Composable
fun IncidentTypeSelectionDialog(
    incidentTypes: List<IncidentType>,
    onDismiss: () -> Unit,
    onIncidentSelected: (IncidentType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reportar Incidente") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(incidentTypes) { incident ->
                    Card(
                        modifier = Modifier.clickable { onIncidentSelected(incident) }.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(incident.icon, null, modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text(incident.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun IncidentDescriptionDialog(
    incidentType: IncidentType,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Describir '${incidentType.name}'") },
        text = {
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Descripción (obligatorio)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { Button(onClick = onConfirm, enabled = description.isNotBlank()) { Text("Reportar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun CategoryChip(text: String, icon: ImageVector) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.clickable { }
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

data class IncidentType(val name: String, val icon: ImageVector)