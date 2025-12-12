package com.inacapsos.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.inacapsos.app.core.AppSession
import com.inacapsos.app.data.repository.InacapRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SosScreen(inacapRepository: InacapRepository) {
    val inacapRed = Color(0xFFCC0000)
    val successGreen = Color(0xFF2E7D32)

    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator }
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Estados
    var isSending by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    // --- ANIMACIONES ---
    val buttonColor by animateColorAsState(
        targetValue = if (isSuccess) successGreen else if (isSending) Color.Gray else inacapRed,
        animationSpec = tween(durationMillis = 500),
        label = "ButtonColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 8f, // Onda gigante
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    // =====================================================================
    // 1. LÓGICA DE ENVÍO
    // =====================================================================
    val sendAlertLogic: () -> Unit = {
        coroutineScope.launch {
            isSending = true
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    var location = fusedLocationClient.lastLocation.await()
                    if (location == null) {
                        location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                    }

                    if (location != null) {
                        val userId = AppSession.userId
                        if (userId != null) {
                            val incidentData = mapOf(
                                "titulo" to "Alerta SOS",
                                "descripcion" to "Botón de pánico presionado.",
                                "latitud" to location.latitude,
                                "longitud" to location.longitude,
                                "tipo" to "sos",
                                "userId" to userId,
                                "timestamp" to System.currentTimeMillis()
                            )
                            inacapRepository.reportIncident(incidentData)

                            // --- ÉXITO ---
                            isSending = false
                            isSuccess = true

                            // INTENTAR VIBRAR (Protegido con Try-Catch)
                            // Si falla la vibración, NO detiene el flujo, permitiendo que el botón se resetee.
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    vibrator.vibrate(500)
                                }
                            } catch (e: Exception) {
                                // Si falla (ej: sin permiso), solo lo ignoramos y seguimos
                                e.printStackTrace()
                            }

                            // ESPERAR Y RESETEAR
                            delay(5000)
                            isSuccess = false

                        } else {
                            isSending = false
                            Toast.makeText(context, "Error: Sesión no válida.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        isSending = false
                        Toast.makeText(context, "No se pudo obtener coordenadas.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                isSending = false
                // Solo mostramos error si no es un error de cancelación normal
                Toast.makeText(context, "Ocurrió un error al enviar.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // =====================================================================
    // 2. MANEJADOR GPS Y PERMISOS
    // =====================================================================
    val gpsResolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            sendAlertLogic()
        } else {
            isSending = false
            Toast.makeText(context, "Es necesario activar el GPS.", Toast.LENGTH_LONG).show()
        }
    }

    fun checkGpsAndSend() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { sendAlertLogic() }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    gpsResolutionLauncher.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    isSending = false
                }
            } else {
                isSending = false
                Toast.makeText(context, "Error GPS no resolvable.", Toast.LENGTH_LONG).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) checkGpsAndSend() else Toast.makeText(context, "Se requiere permiso", Toast.LENGTH_LONG).show()
    }

    // =====================================================================
    // UI (Sin cambios visuales mayores)
    // =====================================================================
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // HEADER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(inacapRed)
                    .padding(vertical = 24.dp, horizontal = 16.dp)
            ) {
                Column {
                    Text("Botón de emergencia", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Usar solo en emergencias reales.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                }
            }

            // CUERPO
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        if (isSuccess) "¡Alerta enviada correctamente!" else "Al presionar, se verificará tu ubicación y se enviará la alerta.",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSuccess) successGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSuccess) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // --- ZONA DEL BOTÓN SOS ---
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(bottom = 60.dp)
                    ) {
                        // 1. EFECTO ONDA EXPANSIVA (Solo visible en éxito)
                        if (isSuccess) {
                            Box(
                                modifier = Modifier
                                    .size(220.dp) // Tamaño base
                                    .scale(pulseScale)
                                    .alpha(pulseAlpha)
                                    .background(successGreen, CircleShape)
                            )
                        }

                        // 2. FONDO ESTÁTICO (Anillo exterior)
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .background(buttonColor.copy(alpha = 0.2f), CircleShape)
                        )

                        // 3. EL BOTÓN PRINCIPAL
                        Button(
                            onClick = {
                                if (isSending || isSuccess) return@Button
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    checkGpsAndSend()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            },
                            modifier = Modifier
                                .size(220.dp)
                                .shadow(if (isSuccess) 0.dp else 15.dp, CircleShape, spotColor = buttonColor),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 15.dp,
                                pressedElevation = 6.dp
                            )
                        ) {
                            // ANIMACIÓN DE CONTENIDO
                            AnimatedContent(
                                targetState = isSuccess,
                                transitionSpec = {
                                    scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                                },
                                label = "ButtonContent"
                            ) { success ->
                                if (success) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(60.dp))
                                        Text("ENVIADO", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                                    }
                                } else if (isSending) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(50.dp))
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(50.dp))
                                        Text("SOS", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}