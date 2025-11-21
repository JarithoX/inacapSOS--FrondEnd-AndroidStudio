package com.inacapsos.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inacapsos.app.core.AppSession
import com.inacapsos.app.data.remote.dto.UpdateUserDto
import com.inacapsos.app.data.repository.InacapRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    repository: InacapRepository,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {

    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var sede by remember { mutableStateOf("") }
    var genero by remember { mutableStateOf("") }

    var isSedeExpanded by remember { mutableStateOf(false) }
    var isGeneroExpanded by remember { mutableStateOf(false) }
    val sedesList = listOf("Renca", "Maipú", "La Granja", "Puente Alto", "Apoquindo", "Santiago Centro", "Santiago Sur", "sede Central", "Ñuñoa", "Otro")
    val generosList = listOf("Masculino", "Femenino", "Otro")

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = AppSession.userId) {
        val userId = AppSession.userId
        if (userId == null) {
            error = "No se pudo identificar al usuario."
            isLoading = false
            return@LaunchedEffect
        }
        try {
            val user = repository.getUserDetails(userId)
            nombre = user.nombre
            apellido = user.apellido ?: ""
            edad = user.edad?.toString() ?: ""
            sede = user.sede ?: ""
            genero = user.genero ?: ""
        } catch (e: Exception) {
            error = "Error al cargar el perfil: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- CABECERA ESTÁTICA ---
        // Esta Box contiene la curva y la información del usuario, pero no se desliza.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp) // Altura fija para la cabecera
                .align(Alignment.TopCenter)
        ) {
            // Curva Roja de Fondo
            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(0f, size.height * 0.8f)
                    quadraticBezierTo(size.width / 2f, size.height * 1.1f, size.width, size.height * 0.8f)
                    lineTo(size.width, 0f)
                    close()
                }
                drawPath(path = path, color = primaryColor)
            }

            // Contenido de la Cabecera (Título, Icono, etc.)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding() // Padding solo para el contenido, no para el fondo
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Icono de perfil",
                    tint = Color.White,
                    modifier = Modifier.size(110.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = AppSession.userName ?: "",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = AppSession.userEmail ?: "",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }

            // Título "Editar Perfil" y Botón de Volver
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .align(Alignment.TopCenter)
                    .padding(start = 4.dp, end = 16.dp)
                    .offset(y = (-15).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White)
                }
                Text(
                    text = "Editar Perfil",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .offset(x = (67).dp)
                )
            }
        }

        // --- SECCIÓN 2: FORMULARIO DESLIZABLE ---
        // Esta Column contiene solo la tarjeta y se puede deslizar.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 270.dp) // Espacio para que empiece justo debajo de la cabecera
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text("Cargando datos...", modifier = Modifier.padding(top = 64.dp))
                    }
                } else {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (error != null) {
                            Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                        }
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = { Text("Nombre") },
                            modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = apellido,
                            onValueChange = { apellido = it },
                            label = { Text("Apellido") },
                            modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = edad,
                            onValueChange = { edad = it },
                            label = { Text("Edad") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Spacer(modifier = Modifier.height(16.dp))

                        ExposedDropdownMenuBox(expanded = isSedeExpanded, onExpandedChange = { isSedeExpanded = it }) {
                            OutlinedTextField(
                                value = sede,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Sede") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSedeExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth())
                            ExposedDropdownMenu(expanded = isSedeExpanded, onDismissRequest = { isSedeExpanded = false }) {
                                sedesList.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { sede = item; isSedeExpanded = false }) }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        ExposedDropdownMenuBox(expanded = isGeneroExpanded, onExpandedChange = { isGeneroExpanded = it }) {
                            OutlinedTextField(
                                value = genero,
                                onValueChange = {}, readOnly = true,
                                label = { Text("Género") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGeneroExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth())
                            ExposedDropdownMenu(expanded = isGeneroExpanded, onDismissRequest = { isGeneroExpanded = false }) {
                                generosList.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { genero = item; isGeneroExpanded = false }) }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    isSaving = true; error = null
                                    try {
                                        val userId = AppSession.userId!!
                                        val edadInt = edad.toIntOrNull()
                                        if (edadInt == null) {
                                            error = "La edad debe ser un número."; isSaving = false; return@launch
                                        }
                                        val updateUserDto = UpdateUserDto(nombre.trim(), apellido.trim(), edadInt, sede, genero)
                                        repository.updateUser(userId, updateUserDto)
                                        // Muestra el mensaje de éxito
                                        snackbarHostState.showSnackbar("Perfil actualizado con éxito")
                                        onSaveSuccess()
                                    } catch (e: Exception) {
                                        error = "No se pudo guardar: ${e.message}"
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Guardar Cambios")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // Espacio al final del scroll
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}