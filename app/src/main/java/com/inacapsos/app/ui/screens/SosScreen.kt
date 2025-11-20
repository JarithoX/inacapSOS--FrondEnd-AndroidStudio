package com.inacapsos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun SosScreen() {
    val inacapRed = Color(0xFFCC0000)

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
                        onClick = {},
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
