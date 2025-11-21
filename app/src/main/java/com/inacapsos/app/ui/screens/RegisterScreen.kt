package com.inacapsos.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.inacapsos.app.R
import com.inacapsos.app.data.remote.dto.RegisterRequestDto
import com.inacapsos.app.data.repository.InacapRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    repository: InacapRepository,
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit
) {
    // --- ESTADO DEL FORMULARIO ---
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") } // <-- NUEVO: Confirmar contraseña
    var edad by remember { mutableStateOf("") }

    // Estado para los menús desplegables
    var sede by remember { mutableStateOf("") }
    var genero by remember { mutableStateOf("") }
    var isSedeExpanded by remember { mutableStateOf(false) }
    var isGeneroExpanded by remember { mutableStateOf(false) }

    // Listas de opciones para los menús
    val sedesList = listOf(
        "Renca", "Maipú", "La Granja", "Puente Alto",
        "Apoquindo", "Santiago Centro", "Santiago Sur",
        "sede Central", "Ñuñoa", "Otro"
    )
    val generosList = listOf("Masculino", "Femenino", "Otro")


    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val curveColor = MaterialTheme.colorScheme.surfaceVariant

        // Fondo superior curvo
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
                quadraticBezierTo(width * 0.5f, height * 1.15f, width, height * 0.8f)
                lineTo(width, 0f)
                close()
            }
            drawPath(path = path, color = curveColor)
        }

        // Logo y sombra
        Box(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp).height(220.dp).fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_inacapsos),
                contentDescription = "Logo InacapSOS",
                modifier = Modifier.size(220.dp).offset(x = (-10).dp)
            )
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(140.dp)
                    .height(26.dp)
                    .offset(x = (6).dp, y = (-9).dp),

            ) { drawOval(color = Color(0x33000000)) }
        }

        // Columna principal del formulario con scroll
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(280.dp))

            // Calcular la opacidad (alpha) basado en la posición del scroll
            val fadeOutDistance = 50.dp
            val fadeOutDistancePx = with(LocalDensity.current) { fadeOutDistance.toPx() }
            val titlesAlpha = (1f - (scrollState.value / fadeOutDistancePx)).coerceIn(0f, 1f)

            Text("Crear Cuenta",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { alpha = titlesAlpha })

            Spacer(modifier = Modifier.height(8.dp))

            Text("Completa tus datos",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { alpha = titlesAlpha })

            Spacer(modifier = Modifier.height(24.dp))

            // --- CAMPOS DEL FORMULARIO (ACTUALIZADOS) ---
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.weight(1f),
                    singleLine = true)

                OutlinedTextField(
                    value = apellido,
                    onValueChange = { apellido = it },
                    label = { Text("Apellido") },
                    modifier = Modifier.weight(1f),
                    singleLine = true)
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true)

            Spacer(modifier = Modifier.height(16.dp))

            // --- NUEVO: Campo de contraseña y confirmación ---
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmar Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true)

            Spacer(modifier = Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // --- NUEVO: Menú desplegable para Sede ---
                ExposedDropdownMenuBox(
                    expanded = isSedeExpanded,
                    onExpandedChange = { isSedeExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = sede,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sede") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSedeExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = isSedeExpanded, onDismissRequest = { isSedeExpanded = false }) {
                        sedesList.forEach { item ->
                            DropdownMenuItem(text = { Text(item) }, onClick = { sede = item; isSedeExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = edad, onValueChange = { edad = it }, label = { Text("Edad") }, modifier = Modifier.weight(0.6f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- NUEVO: Menú desplegable para Género ---
            ExposedDropdownMenuBox(expanded = isGeneroExpanded, onExpandedChange = { isGeneroExpanded = it }) {
                OutlinedTextField(
                    value = genero,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Género") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGeneroExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = isGeneroExpanded, onDismissRequest = { isGeneroExpanded = false }, modifier = Modifier.fillMaxWidth()) {
                    generosList.forEach { item ->
                        DropdownMenuItem(text = { Text(item) }, onClick = { genero = item; isGeneroExpanded = false })
                    }
                }
            }


            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }
            Spacer(modifier = Modifier.height(32.dp))

            // --- LÓGICA DE REGISTRO ACTUALIZADA ---
            Button(
                onClick = {
                    // --- NUEVO: Validación de contraseña ---
                    if (password != confirmPassword) {
                        error = "Las contraseñas no coinciden."
                        return@Button
                    }
                    scope.launch {
                        isLoading = true; error = null
                        try {
                            val edadInt = edad.toIntOrNull()
                            if (edadInt == null) {
                                error = "La edad debe ser un número válido."; isLoading = false; return@launch
                            }
                            val request = RegisterRequestDto(
                                nombre = nombre.trim(), apellido = apellido.trim(), email = email.trim(),
                                password = password, edad = edadInt, sede = sede.trim(), genero = genero.trim()
                            )
                            repository.register(request)
                            onRegisterSuccess()
                        } catch (e: Exception) {
                            error = e.message ?: "Ocurrió un error inesperado."
                        } finally {
                            isLoading = false
                        }
                    }
                },
                // Habilitado solo si todos los campos están llenos
                enabled = !isLoading && listOf(nombre, apellido, email, password, confirmPassword, edad, sede, genero).all { it.isNotBlank() },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Registrarme")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- SOLUCIÓN: Botón para volver atrás ---
        // Se dibuja al final para que quede por encima de todo.
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 24.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Volver atrás", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}
