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
import com.inacapsos.app.data.repository.InacapRepositoryImpl
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val repository = remember { InacapRepositoryImpl() }
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("testuserR@inacapsos.cl") }
    var password by remember { mutableStateOf("1234") }
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
            // MOD: Box para apilar logo + sombra elíptica bajo los pies
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
                            val response = repository.login(email.trim(), password)
                            if (response.user != null) {
                                AppSession.userId = response.user.id
                                AppSession.userName = response.user.nombre
                                onLoginSuccess()
                            } else {
                                error = response.message ?: "Usuario o contraseña incorrectos"
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "No se pudo iniciar sesión"
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
                TextButton(onClick = { /* Acción de registro deshabilitada por ahora */ }) {
                    Text("Regístrate", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}