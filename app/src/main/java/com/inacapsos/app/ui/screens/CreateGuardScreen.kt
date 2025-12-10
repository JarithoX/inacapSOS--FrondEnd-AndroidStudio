package com.inacapsos.app.ui.screens

import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.inacapsos.app.data.remote.dto.CreateGuardRequestDto
import com.inacapsos.app.data.repository.InacapRepository
import kotlinx.coroutines.launch

private fun isEmailValid(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGuardScreen(
    repository: InacapRepository,
    onGuardCreated: () -> Unit,
    onBack: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var isEmailError by remember { mutableStateOf(false) }
    var isPasswordError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Cuenta de Guardia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Datos del Nuevo Guardia",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = apellido,
                onValueChange = { apellido = it },
                label = { Text("Apellido") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    isEmailError = !isEmailValid(it)
                },
                label = { Text("Correo Electrónico") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = isEmailError,
                supportingText = {
                    if (isEmailError) {
                        Text("Por favor, introduce un correo electrónico válido.")
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    isPasswordError = it.length < 8
                },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = isPasswordError,
                supportingText = {
                    if (isPasswordError) {
                        Text("La contraseña debe tener al menos 8 caracteres.")
                    }
                }
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    isEmailError = !isEmailValid(email)
                    isPasswordError = password.length < 8
                    if (isEmailError || isPasswordError) {
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        error = null
                        try {
                            val request = CreateGuardRequestDto(
                                nombre = nombre.trim(),
                                apellido = apellido.trim(),
                                email = email.trim(),
                                password = password,
                                rolId = "3"
                            )

                            repository.createGuard(request)
                            onGuardCreated()

                        } catch (e: Exception) {
                            Log.e("CreateGuardScreen", "Error al crear guardia", e)
                            val errorMessage = e.message
                            if (errorMessage != null && ("500" in errorMessage || "Internal Server Error" in errorMessage)) {
                                error = "El correo electrónico ya podría estar en uso o hubo un problema en el servidor. Intente nuevamente."
                            } else {
                                error = errorMessage ?: "Ocurrió un error inesperado."
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && listOf(nombre, apellido, email, password).all { it.isNotBlank() } && !isEmailError && !isPasswordError,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Crear Cuenta")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
