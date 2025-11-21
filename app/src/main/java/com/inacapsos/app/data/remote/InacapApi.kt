package com.inacapsos.app.data.remote

import com.inacapsos.app.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.PUT
import retrofit2.http.Path

interface InacapApi {

    @POST("usuarios/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    @POST("usuarios/register")
    suspend fun register(@Body request: RegisterRequestDto)

    @GET("usuarios/{id}")
    suspend fun getUserDetails(@Path("id") userId: String): UserDto

    @PUT("usuarios/{id}")
    suspend fun updateUser(@Path("id") userId: String, @Body body: UpdateUserDto)

    @POST("usuarios/create-guard")
    suspend fun createGuard(@Body request: CreateGuardRequestDto)

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
