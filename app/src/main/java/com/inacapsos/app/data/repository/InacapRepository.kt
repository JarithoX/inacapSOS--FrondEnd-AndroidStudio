package com.inacapsos.app.data.repository

import com.inacapsos.app.data.remote.dto.IncidenteDto
import com.inacapsos.app.data.remote.dto.LoginResponseDto
import com.inacapsos.app.data.remote.dto.ReportDto

interface InacapRepository {
    suspend fun login(email: String, password: String): LoginResponseDto
    suspend fun getReportes(usuarioId: String?): List<ReportDto>
    suspend fun reportIncident(incidentData: Map<String, Any>)
    suspend fun getIncidentes(): List<IncidenteDto>
}
