package com.inacapsos.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Representa la estructura de un incidente tal como se lee desde Firebase.
 * Esta es la única estructura de datos que la app manejará.
 */
data class IncidenteDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("descripcion")
    val descripcion: String,

    @SerializedName("latitud")
    val latitud: Double,

    @SerializedName("longitud")
    val longitud: Double,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("timestamp")
    val timestamp: FechaDto,

    @SerializedName("estado")
    val estado: String,

    // Se hace opcional (nullable) porque no todos los reportes tendrán evidencia.
    @SerializedName("evidencia_url")
    val evidenciaUrl: String? = null,

    // motivo de la cancelación
    @SerializedName("motivo_cancelacion")
    val motivo_cancelacion: String? = null,

    @SerializedName("tieneComentarios")
    val tieneComentarios: Boolean = false
)

/**
 * Representa la estructura del objeto de marca de tiempo que devuelve Firebase.
 */
data class FechaDto(
    @SerializedName("_seconds")
    val seconds: Long,

    @SerializedName("_nanoseconds")
    val nanoseconds: Long
)
