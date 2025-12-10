package com.inacapsos.app.data.model

data class Guardia(
    val id: String = "",       // El ID único del documento en Firebase
    val nombre: String = "",   // Debe coincidir con el campo "nombre" en Firebase
    val email: String = ""     // Debe coincidir con el campo "email" en Firebase
)