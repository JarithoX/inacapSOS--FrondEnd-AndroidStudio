package com.inacapsos.app.data.repository

import com.inacapsos.app.data.remote.ApiClient
import com.inacapsos.app.data.remote.dto.IncidenteDto
import com.inacapsos.app.data.remote.dto.LoginRequestDto
import com.inacapsos.app.data.remote.dto.LoginResponseDto
import com.inacapsos.app.data.remote.dto.ReportDto

class InacapRepositoryImpl : InacapRepository {

    private val api = ApiClient.api

    override suspend fun login(email: String, password: String): LoginResponseDto {
        return api.login(LoginRequestDto(email = email, contrasena = password))
    }

    override suspend fun getReportes(usuarioId: String?): List<ReportDto> {
        return api.getReportes(usuarioId = usuarioId)
    }

    override suspend fun reportIncident(incidentData: Map<String, Any>) {
        api.reportIncident(incidentData = incidentData)
    }

    override suspend fun getIncidentes(): List<IncidenteDto> {
        return api.getIncidentes()
    }
}
