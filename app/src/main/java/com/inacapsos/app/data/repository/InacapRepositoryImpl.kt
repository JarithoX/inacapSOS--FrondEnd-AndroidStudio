package com.inacapsos.app.data.repository

import com.inacapsos.app.core.AppSession
import com.inacapsos.app.data.remote.InacapApi
import com.inacapsos.app.data.remote.dto.*

class InacapRepositoryImpl(private val api: InacapApi) : InacapRepository {

    override suspend fun login(request: LoginRequestDto): LoginResponseDto {
        val response = api.login(request)
        if (response.isSuccessful) {
            val token = response.headers()["Authorization"]?.removePrefix("Bearer ")
            if (token != null) {
                AppSession.token = token
            }
            return response.body()!!
        } else {
            throw Exception("Error en el inicio de sesión: ${response.code()}")
        }
    }

    override suspend fun register(request: RegisterRequestDto) {
        return api.register(request)
    }

    override suspend fun getUserDetails(userId: String): UserDto {
        return api.getUserDetails(userId)
    }

    override suspend fun getUsers(): List<UserDto> {
        return api.getUsers()
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
    override suspend fun updateIncidenteState(id: String, nuevoEstado: String, motivo: String?): Boolean {
        return try {
            val body = mutableMapOf("estado" to nuevoEstado)
            if (motivo != null) {
                body["motivo_cancelacion"] = motivo
            }

            val response = api.updateIncidente(id, body)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
