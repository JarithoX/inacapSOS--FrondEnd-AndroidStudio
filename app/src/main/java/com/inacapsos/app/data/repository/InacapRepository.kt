package com.inacapsos.app.data.repository

import com.inacapsos.app.data.remote.dto.*

interface InacapRepository {
    suspend fun login(request: LoginRequestDto): LoginResponseDto
    suspend fun register(request: RegisterRequestDto)
    suspend fun getUserDetails(userId: String): UserDto
    suspend fun getUsers(): List<UserDto>
    suspend fun updateUser(userId: String, body: UpdateUserDto)
    suspend fun createGuard(request: CreateGuardRequestDto)
    suspend fun getReportes(usuarioId: String?): List<ReportDto>
    suspend fun reportIncident(incidentData: Map<String, Any>)
    suspend fun getIncidentes(): List<IncidenteDto>
    suspend fun updateIncidenteState(id: String, nuevoEstado: String, motivo: String? = null): Boolean
}
