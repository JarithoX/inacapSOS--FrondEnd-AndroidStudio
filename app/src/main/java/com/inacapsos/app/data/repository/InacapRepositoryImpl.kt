package com.inacapsos.app.data.repository

import com.inacapsos.app.data.remote.ApiClient
import com.inacapsos.app.data.remote.InacapApi
import com.inacapsos.app.data.remote.dto.*

class InacapRepositoryImpl(api1: InacapApi) : InacapRepository {

    private val api = ApiClient.api

    override suspend fun login(request: LoginRequestDto): LoginResponseDto {
        return api.login(request)
    }

    override suspend fun register(request: RegisterRequestDto) {
        return api.register(request)
    }

    override suspend fun getUserDetails(userId: String): UserDto {
        return api.getUserDetails(userId)
    }

    override suspend fun updateUser(userId: String, body: UpdateUserDto) {
        return api.updateUser(userId, body)
    }

    override suspend fun createGuard(request: CreateGuardRequestDto) {
        return api.createGuard(request)
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
