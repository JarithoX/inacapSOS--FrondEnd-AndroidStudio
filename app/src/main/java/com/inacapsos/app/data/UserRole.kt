package com.inacapsos.app.data

import com.google.gson.annotations.SerializedName

enum class UserRole {
    @SerializedName("GUARDIA")
    GUARD,
    @SerializedName("estudiante")
    STUDENT,
    @SerializedName("ADMIN")
    ADMIN
}
