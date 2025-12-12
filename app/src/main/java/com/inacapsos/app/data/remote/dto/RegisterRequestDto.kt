package com.inacapsos.app.data.remote.dto

data class RegisterRequestDto(
    val nombre: String,
    val apellido: String,
    val email: String,
    val password: String,
    val edad: Int,
    val sede: String,
    val genero: String
)
