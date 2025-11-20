package com.inacapsos.app.data.remote.dto

data class ReportDto(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val tipo: String,
    val fecha: String,
    val estado: String
)

// NO SE ESTA UTILIZANDO, RECOMIENDO BORRARLO Y SOLO UTILIZAR
// EL ARCHIVO 'IncidenteDto.kt'