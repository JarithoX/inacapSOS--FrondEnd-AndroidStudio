package com.inacapsos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inacapsos.app.core.AppSession

// ============================================================
// 1. FUNCIÓN PRINCIPAL
// ============================================================
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onLogin: () -> Unit,
    onEditProfile: () -> Unit // <-- Parámetro para la acción de editar
) {
    val isLoggedIn = AppSession.userId != null

    if (isLoggedIn) {
        // Si está logueado, llama a la vista de usuario
        LoggedInProfileView(
            onLogout = onLogout,
            onEditProfile = onEditProfile // <-- Pasamos la acción
        )
    } else {
        // Si no, llama a la vista de invitado
        GuestProfileView(onLogin = onLogin)
    }
}

// ============================================================
// 2. VISTA PARA EL USUARIO LOGUEADO 
// ============================================================
@Composable
private fun LoggedInProfileView(
    onLogout: () -> Unit,
    onEditProfile: () -> Unit // Recibe la acción de editar
) {
    val inacapRed = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // --- Encabezado Rojo ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = inacapRed,
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Mi Perfil",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.size(60.dp),
                        tint = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                //  Muestra el nombre real del usuario de AppSession
                Text(
                    text = AppSession.userName ?: "Estudiante Inacap",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = AppSession.userEmail ?: "correo@inacapmail.cl",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // --- Lista de Opciones (Menu) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(15.dp))

            ProfileOptionItem(
                icon = Icons.Default.Edit,
                text = "Editar Perfil",
                onClick = onEditProfile
            )
            ProfileOptionItem(
                icon = Icons.Default.Notifications,
                text = "Notificaciones",
                onClick = {}
            )
            ProfileOptionItem(
                icon = Icons.Default.Lock,
                text = "Seguridad",
                onClick = {}
            )
            ProfileOptionItem(
                icon = Icons.Default.Info,
                text = "Ayuda y Soporte",
                onClick = {}
            )

            Spacer(modifier = Modifier.height(145.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "Cerrar Sesión",
                    color = inacapRed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============================================================
// 3. VISTA PARA EL INVITADO 
// ============================================================
@Composable
private fun GuestProfileView(onLogin: () -> Unit) {
    val inacapRed = Color(0xFFD52B1E)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Modo Invitado",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Estás navegando como invitado. Para acceder a tu historial académico, " +
                        "credencial virtual y reportar incidentes, necesitas identificarte.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = inacapRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Iniciar Sesión",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============================================================
// 4. ITEM DE OPCIÓN REUTILIZABLE 
// ============================================================
@Composable
fun ProfileOptionItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // <-- Llama a la acción onClick
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
    }
}