package com.inacapsos.app.ui.screens

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.inacapsos.app.core.AppSession
import com.inacapsos.app.data.repository.InacapRepository
import com.inacapsos.app.data.remote.dto.UpdateUserDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    repository: InacapRepository,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    // --- ESTADO DEL FORMULARIO ---
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") } // El email no será editable
    var edad by remember { mutableStateOf("") }
    var sede by remember { mutableStateOf("") }
    var genero by remember { mutableStateOf("") }

    // Estado para los menús desplegables
    var isSedeExpanded by remember { mutableStateOf(false) }
    var isGeneroExpanded by remember { mutableStateOf(false) }
    val sedesList = listOf(
        "Apoquindo", "Maipú", "Pérez Rosales", "Puente Alto",
        "Renca", "San Bernardo", "Santiago Centro", "Santiago Sur",
        "República", "Otro"
    )
    val generosList = listOf("Masculino", "Femenino", "Otro")

    // Estado de la UI
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // --- Carga inicial de datos ---
    LaunchedEffect(key1 = AppSession.userId) {
        val userId = AppSession.userId
        if (userId == null) {
            error = "No se pudo identificar al usuario."
            isLoading = false
            return@LaunchedEffect
        }
        try {
            // NOTA: getUserDetails es una función que crearemos en el repositorio
            val user = repository.getUserDetails(userId)
            if (user != null) {
                nombre = user.nombre
                // Suponiendo que el objeto UserDto completo tiene estos campos
                apellido = user.apellido ?: ""
                email = user.email
                edad = user.edad?.toString() ?: ""
                sede = user.sede ?: ""
                genero = user.genero ?: ""
            } else {
                error = "No se pudieron cargar los datos del usuario."
            }
        } catch (e: Exception) {
            error = "Error al cargar el perfil: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Mi Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                error = null
                                try {
                                    val userId = AppSession.userId!!
                                    val edadInt = edad.toIntOrNull()
                                    if (edadInt == null) {
                                        error = "La edad debe ser un número."
                                        isSaving = false
                                        return@launch
                                    }

                                    val updateUserDto = UpdateUserDto(
                                        nombre = nombre.trim(),
                                        apellido = apellido.trim(),
                                        edad = edadInt,
                                        sede = sede,
                                        genero = genero
                                    )
                                    repository.updateUser(userId, updateUserDto)


                                    onSaveSuccess()

                                } catch (e: Exception) {
                                    error = "No se pudo guardar: ${e.message}"
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isLoading && !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("Guardar")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                }

                // --- Campos del Formulario ---
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = apellido, onValueChange = { apellido = it }, label = { Text("Apellido") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = email, onValueChange = {}, label = { Text("Correo Electrónico (no editable)") }, modifier = Modifier.fillMaxWidth(), readOnly = true, enabled = false)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = edad, onValueChange = { edad = it }, label = { Text("Edad") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(modifier = Modifier.height(16.dp))

                // Menú para Sede
                ExposedDropdownMenuBox(expanded = isSedeExpanded, onExpandedChange = { isSedeExpanded = it }) {
                    OutlinedTextField(value = sede, onValueChange = {}, readOnly = true, label = { Text("Sede") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSedeExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = isSedeExpanded, onDismissRequest = { isSedeExpanded = false }) {
                        sedesList.forEach { item ->
                            DropdownMenuItem(text = { Text(item) }, onClick = { sede = item; isSedeExpanded = false })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Menú para Género
                ExposedDropdownMenuBox(expanded = isGeneroExpanded, onExpandedChange = { isGeneroExpanded = it }) {
                    OutlinedTextField(value = genero, onValueChange = {}, readOnly = true, label = { Text("Género") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGeneroExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = isGeneroExpanded, onDismissRequest = { isGeneroExpanded = false }) {
                        generosList.forEach { item ->
                            DropdownMenuItem(text = { Text(item) }, onClick = { genero = item; isGeneroExpanded = false })
                        }
                    }
                }
            }
        }
    }
}