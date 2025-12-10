package com.inacapsos.app.data.remote.dto

data class UserDto(
    val id: String,
    val nombre: String,
    val apellido: String?,
    val email: String,
    val rol: String,
    val edad: Int?,
    val sede: String?,
    val genero: String?
)