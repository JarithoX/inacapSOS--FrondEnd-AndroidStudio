package com.inacapsos.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.preference.PreferenceManager
import com.google.android.gms.location.LocationServices
import com.inacapsos.app.R
import com.inacapsos.app.core.AppSession
import com.inacapsos.app.data.remote.dto.IncidenteDto
import com.inacapsos.app.data.repository.InacapRepositoryImpl
import com.inacapsos.app.navigation.Screen
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.graphics.Color

// =======================================================================================
// FUNCIÓN AUXILIAR PARA OBTENER EL ÍCONO DEL MARCADOR
// Devuelve el ícono correcto según el título del incidente.
// =======================================================================================
private fun getMarkerIconForIncident(context: Context, incidentTitle: String): Drawable? {
    val drawableId = when (incidentTitle) {
        // Marcadores específicos
        "Emergencia Seguridad" -> R.drawable.marker_reporte_emergencia
        "Emergencia Médica" -> R.drawable.marker_reporte_emergencia_medica

        // Marcadores de colores por defecto para los demás tipos
        "Incendio" -> R.drawable.marker_red
        "Robo" -> R.drawable.marker_red
        "Acoso" -> R.drawable.marker_yellow
        "Otro" -> R.drawable.marker_green

        // Un ícono por si acaso no coincide ninguno
        else -> R.drawable.marker_report_error
    }
    return ContextCompat.getDrawable(context, drawableId)
}

@Composable
fun MapScreen(navController: NavHostController) {
    val context = LocalContext.current
    val repository = remember { InacapRepositoryImpl() }
    val scope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var incidentes by remember { mutableStateOf<List<IncidenteDto>>(emptyList()) }
    var showMapMenu by remember { mutableStateOf(false) }

    // --- ESTADOS PARA EL DIÁLOGO DE REPORTE ---
    var selectedIncidentType by remember { mutableStateOf<IncidentType?>(null) }
    var incidentDescription by remember { mutableStateOf("") }
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // --- FusedLocationProviderClient PARA OBTENER LA UBICACIÓN ---
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // --- LAUNCHER PARA SOLICITAR PERMISOS DE UBICACIÓN ---
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            // Permiso concedido, obtener la ubicación
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        userLocation = GeoPoint(location.latitude, location.longitude)
                    }
                }
            }
        } else {
            error = "El permiso de ubicación es necesario para reportar un incidente."
        }
    }

    // --- OBTENER LA UBICACIÓN AL ENTRAR EN LA PANTALLA ---
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = GeoPoint(location.latitude, location.longitude)
                }
            }
        }
        // Cargar incidentes
        isLoading = true
        try {
            incidentes = repository.getIncidentes()
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar los incidentes."
        } finally {
            isLoading = false
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
    Configuration.getInstance().userAgentValue = "com.inacapsos.app"

    Box(modifier = Modifier.fillMaxSize()) {
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
            update = {  } // Logica desplazada mas abajo
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clickable { /* Acción simulada para la demo */ },
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Buscar punto de interes...", style = MaterialTheme.typography.bodyLarge, color = Color.Gray, modifier = Modifier.weight(1f))
                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Perfil", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                CategoryChip(text = "Reportes", icon = Icons.Default.Info)
                CategoryChip(text = "Puntos de peligro", icon = Icons.Default.Warning)
                CategoryChip(text = "Seguridad", icon = Icons.Default.Security)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 120.dp, end = 16.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { showMapMenu = true },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Default.Map, contentDescription = "Capas")
            }
            DropdownMenu(expanded = showMapMenu, onDismissRequest = { showMapMenu = false }, modifier = Modifier.align(Alignment.CenterEnd)) {
                DropdownMenuItem(text = { Text("Mapa Normal") }, onClick = {
                    mapView?.setTileSource(TileSourceFactory.MAPNIK)
                    showMapMenu = false
                })
                DropdownMenuItem(text = { Text("Mapa Satelital (Demo)") }, onClick = {
                    mapView?.setTileSource(TileSourceFactory.HIKEBIKEMAP)
                    showMapMenu = false
                })
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(onClick = {
                mapView?.controller?.zoomIn() }, containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }
            SmallFloatingActionButton(onClick = {
                mapView?.controller?.zoomOut() }, containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }
            SmallFloatingActionButton(onClick = {
                mapView?.controller?.animateTo(inacapRencaLocation)
                mapView?.controller?.setZoom(18.5)
            }, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                Icon(Icons.Default.Home, contentDescription = "Centrar")
            }
            SmallFloatingActionButton(onClick = {
                userLocation?.let { loc -> mapView?.controller?.animateTo(loc)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Mi Ubicación")
            }
        }

        FloatingActionButton(
            onClick = {
                if (AppSession.userId != null) {
                    showDialog = true
                } else {
                    navController.navigate(Screen.Login.route)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Report, contentDescription = "Reportar Incidente")
        }

        // --- DIÁLOGO DE REPORTE MEJORADO ---
        if (showDialog) {
            if (selectedIncidentType == null) {
                // FASE 1: Elegir tipo de incidente
                IncidentTypeSelectionDialog(
                    incidentTypes = incidentTypes,
                    onDismiss = { showDialog = false },
                    onIncidentSelected = { incident ->
                        selectedIncidentType = incident
                    }
                )
            } else {
                // FASE 2: Añadir descripción
                IncidentDescriptionDialog(
                    incidentType = selectedIncidentType!!,
                    onDismiss = {
                        showDialog = false
                        selectedIncidentType = null // Reset
                        incidentDescription = "" // Reset
                    },
                    onConfirm = {
                        if (userLocation == null) {
                            error = "No se ha podido obtener la ubicación. Asegúrate de que el GPS está activado."
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
                                    "estado" to "activa",
                                    "evidencia_url" to ""
                                )
                                repository.reportIncident(incidenteData)
                                incidentes = repository.getIncidentes() // Recargar lista
                                showDialog = false
                                selectedIncidentType = null
                                incidentDescription = ""
                            } catch (e: Exception) {
                                error = e.message ?: "Ocurrió un error inesperado al reportar."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    description = incidentDescription,
                    onDescriptionChange = { incidentDescription = it }
                )
            }
        }


        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        error?.let {
            AlertDialog(
                onDismissRequest = { error = null },
                title = { Text("Error") },
                text = { Text(it) },
                confirmButton = {
                    Button(onClick = { error = null }) {
                        Text("Aceptar")
                    }
                }
            )
        }
    }

    LaunchedEffect(incidentes, userLocation, mapView) {
        val map = mapView ?: return@LaunchedEffect

        // 1. Limpiamos los marcadores antiguos para no duplicarlos
        map.overlays.clear()

        // 2. Marcador de INACAP (siempre visible)
        val inacapMarker = Marker(map)
        inacapMarker.position = inacapRencaLocation
        inacapMarker.title = "Sede INACAP Renca"
        inacapMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        ContextCompat.getDrawable(context, R.drawable.marker_icon_renca_v2)?.let { inacapMarker.icon = it }
        map.overlays.add(inacapMarker)

        // 3. Marcador de la ubicación del USUARIO
        userLocation?.let { loc ->
            val userMarker = Marker(map)
            userMarker.position = loc
            userMarker.title = "Tu ubicación"
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            ContextCompat.getDrawable(context, R.drawable.marker_user)?.let { userMarker.icon = it }
            map.overlays.add(userMarker)
        }

        // 4. Marcadores para los INCIDENTES de la base de datos
        incidentes.forEach { incidente ->
            if (incidente.latitud != null && incidente.longitud != null) {
                val marker = Marker(map)
                marker.position = GeoPoint(incidente.latitud, incidente.longitud)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = incidente.titulo
                marker.snippet = incidente.descripcion

                // Asignamos el ícono personalizado
                marker.icon = getMarkerIconForIncident(context, incidente.titulo ?: "Otro")

                map.overlays.add(marker)
            }
        }

        // 5. Forzamos al mapa a redibujarse con los nuevos marcadores
        map.invalidate()
    }
}



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
                        modifier = Modifier
                            .clickable { onIncidentSelected(incident) }
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(incident.icon, contentDescription = null, modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = incident.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
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
        confirmButton = {
            Button(onClick = onConfirm, enabled = description.isNotBlank()) {
                Text("Reportar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}


@Composable
fun CategoryChip(text: String, icon: ImageVector) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.clickable { }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

data class IncidentType(val name: String, val icon: ImageVector)
