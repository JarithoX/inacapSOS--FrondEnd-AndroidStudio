package com.inacapsos.app.data.remote.dto

data class CreateGuardRequestDto(
    val nombre: String,
    val apellido: String,
    val email: String,
    val password: String
)
