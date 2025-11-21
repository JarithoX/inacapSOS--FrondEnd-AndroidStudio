package com.inacapsos.app.data.remote.dto

import com.inacapsos.app.data.UserRole

data class UserDto(
    val id: String,
    val nombre: String,
    val email: String,
    val rol: UserRole
)
