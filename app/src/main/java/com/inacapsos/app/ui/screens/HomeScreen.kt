package com.inacapsos.app.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inacapsos.app.core.AppSession

// Modelo de datos simulado (Esto vendría de tu API)
data class ReporteResumen(
    val id: String,
    val titulo: String,
    val estado: String, // "Pendiente", "En proceso", "Cerrado"
    val fecha: String
)

@Composable
fun HomeScreen(
    userName: String = AppSession.userName ?: "",
    userSede: String = "Sede Renca", // Dato vital de la API
    reportes: List<ReporteResumen> = listOf(), // Cambia a una lista con datos para probar
    onNavigateToMap: () -> Unit = {},

) {
    val inacapRed = Color(0xFFCC0000)
    val bgColor = Color(0xFFF4F6F8)

    Scaffold(
        containerColor = bgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. HEADER CONTEXTUAL (Sincronizable con API)
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

            // 2. CUERPO SCROLLABLE
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 80.dp) // Espacio para BottomBar
            ) {

                // SECCIÓN DE ACCIONES RÁPIDAS
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
                            title = "Números\nEmergencia",
                            icon = Icons.Default.Call,
                            color = Color(0xFF2E7D32), // Verde teléfono
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // SECCIÓN DE ACTIVIDAD RECIENTE (Sincronizado con API)
                item {
                    Text(
                        "Mis Reportes Recientes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (reportes.isEmpty()) {
                    item {
                        EmptyStateCard()
                    }
                } else {
                    items(reportes) { reporte ->
                        ReportItemCard(reporte)
                    }
                }
            }
        }
    }
}

// COMPONENTES REUTILIZABLES

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Todo tranquilo",
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )
            Text(
                "No tienes reportes activos en este momento.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ReportItemCard(reporte: ReporteResumen) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = reporte.titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = reporte.fecha,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Badge de Estado
            Surface(
                color = when(reporte.estado) {
                    "Pendiente" -> Color(0xFFFF9800).copy(alpha = 0.1f)
                    "En proceso" -> Color(0xFF2196F3).copy(alpha = 0.1f)
                    else -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = reporte.estado,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = when(reporte.estado) {
                        "Pendiente" -> Color(0xFFE65100)
                        "En proceso" -> Color(0xFF0D47A1)
                        else -> Color(0xFF1B5E20)
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}