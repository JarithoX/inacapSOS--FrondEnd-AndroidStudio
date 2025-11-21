package com.inacapsos.app.data.remote.dto

data class UpdateUserDto(
    val nombre: String,
    val apellido: String,
    val edad: Int,
    val sede: String,
    val genero: String
)