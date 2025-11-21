package com.inacapsos.app.data.remote

import com.inacapsos.app.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface InacapApi {

    @POST("usuarios/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    @POST("usuarios/register")
    suspend fun register(@Body request: RegisterRequestDto)

    @GET("reportes")
    suspend fun getReportes(
        @Query("usuarioId") usuarioId: String? = null
    ): List<ReportDto>

    @POST("incidente")
    suspend fun reportIncident(
        @Body incidentData: Map<String, @JvmSuppressWildcards Any>
    )

    @GET("incidente")
    suspend fun getIncidentes(): List<IncidenteDto>
}
