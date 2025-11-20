package com.inacapsos.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginResponseDto(
    val message: String?,
    @SerializedName("usuario")
    val user: UserDto?
)
