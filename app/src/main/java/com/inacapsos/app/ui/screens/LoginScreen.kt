package com.inacapsos.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.inacapsos.app.R
import com.inacapsos.app.core.AppSession
import com.inacapsos.app.data.remote.dto.LoginRequestDto
import com.inacapsos.app.data.repository.InacapRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    repository: InacapRepository,
    onLoginSuccess: (String) -> Unit,
    onRegisterClick: () -> Unit
) {

    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("jarod.pinto@inacapmail.cl") }
    var password by remember { mutableStateOf("jarod123") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val curveColor = MaterialTheme.colorScheme.surfaceVariant
        // Fondo superior con curva
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.TopCenter)
        ) {
            val width = size.width
            val height = size.height

            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(0f, height * 0.75f)
                quadraticBezierTo(
                    width * 0.5f,
                    height * 1.15f,   // controla la profundidad de la curva
                    width,
                    height * 0.8f
                )
                lineTo(width, 0f)
                close()
            }

            // color claro tipo “panel” del mockup
            drawPath(
                path = path,
                color = curveColor
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Para apilar logo + sombra elíptica bajo los pies
            Box(
                modifier = Modifier
                    .padding(top = 32.dp, bottom = 24.dp)
                    .height(220.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_inacapsos),
                    contentDescription = "Logo de la aplicación",
                    modifier = Modifier
                        .size(220.dp)
                        .offset(x = (-10).dp)
                )
                Canvas(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .width(140.dp)
                        .height(26.dp)
                        .offset( x = (6).dp, y = (-9).dp)
                ) {
                    drawOval(
                        color = Color(0x33000000) // negro con alpha (~20%)
                    )
                }
            }

            Text(
                text = "Iniciar Sesión",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bienvenido a InacapSOS",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        error = null
                        try {
                            // 1. AUTENTICACIÓN DIRECTA CON FIREBASE
                            val auth = FirebaseAuth.getInstance()
                            val authResult = auth.signInWithEmailAndPassword(email.trim(), password.trim()).await()
                            val firebaseUser = authResult.user

                            if (firebaseUser != null) {
                                // 2. OBTENER EL TOKEN REAL
                                val tokenResult = firebaseUser.getIdToken(true).await()
                                val realToken = tokenResult.token

                                // 3. GUARDAR EL TOKEN (La llave maestra)
                                AppSession.token = realToken
                                AppSession.userEmail = firebaseUser.email
                                AppSession.userId = firebaseUser.uid

                                android.util.Log.d("LOGIN_FIREBASE", "✅ Token Oficial: $realToken")

                                // 4. RECUPERAR EL ROL (El puente)
                                val usuarios = repository.getUsers()

                                val miUsuario = usuarios.find { it.email.equals(email.trim(), ignoreCase = true) }

                                if (miUsuario != null) {
                                    AppSession.userRole = miUsuario.rol
                                    AppSession.userName = miUsuario.nombre
                                    AppSession.save() // Guardamos todo en el celular

                                    onLoginSuccess(miUsuario.rol) // Navegamos a la pantalla correcta
                                } else {
                                    error = "Usuario autenticado, pero no tiene rol asignado en la base de datos."
                                    AppSession.clear() // Limpiamos por seguridad
                                }
                            }
                        } catch (e: Exception) {
                            error = "Error: ${e.message}"
                            android.util.Log.e("LOGIN_ERROR", "Fallo login", e)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Ingresar", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿No tienes una cuenta?")
                TextButton(onClick = { onRegisterClick() }) {
                    Text("Regístrate", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
