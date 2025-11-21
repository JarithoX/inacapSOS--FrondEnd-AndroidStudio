package com.inacapsos.app.data.repository

import com.inacapsos.app.data.remote.dto.*

interface InacapRepository {
    suspend fun login(email: String, password: String): LoginResponseDto
    suspend fun register(request: RegisterRequestDto)
    suspend fun createGuard(request: CreateGuardRequestDto)
    suspend fun getReportes(usuarioId: String?): List<ReportDto>
    suspend fun reportIncident(incidentData: Map<String, Any>)
    suspend fun getIncidentes(): List<IncidenteDto>
}
