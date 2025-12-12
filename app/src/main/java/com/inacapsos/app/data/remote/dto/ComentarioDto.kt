package com.inacapsos.app.data.remote.dto

data class ComentarioDto(
    val id: String? = null,
    val texto: String = "",
    val userId: String = "",
    val nombreUsuario: String = "Anónimo",
    val timestamp: FechaDto? = null
)