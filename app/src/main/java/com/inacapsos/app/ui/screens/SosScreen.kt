package com.inacapsos.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.inacapsos.app.core.AppSession
import com.inacapsos.app.data.repository.InacapRepository
import kotlinx.coroutines.launch

@Composable
fun SosScreen(inacapRepository: InacapRepository) {
    val inacapRed = Color(0xFFCC0000)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                        "Botón de emergencia",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Usar solo en emergencias reales.",
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // 🩶 FONDO GRIS ELEGANTE
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Text(
                        "Esto enviará una alerta a los canales internos.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val userId = AppSession.userId
                                if (userId != null) {
                                    val incidentData = mapOf(
                                        "titulo" to "Alerta SOS",
                                        "descripcion" to "Botón de pánico presionado",
                                        "latitud" to -33.44889, // TODO: Reemplazar con la ubicación real
                                        "longitud" to -70.669265, // TODO: Reemplazar con la ubicación real
                                        "tipo" to "sos",
                                        "userId" to userId
                                    )
                                    try {
                                        inacapRepository.reportIncident(incidentData)
                                        Toast.makeText(context, "Alerta SOS enviada.", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error al enviar la alerta: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, "No se pudo obtener la información del usuario.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.size(150.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(inacapRed)
                    ) {
                        Text(
                            "SOS",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
