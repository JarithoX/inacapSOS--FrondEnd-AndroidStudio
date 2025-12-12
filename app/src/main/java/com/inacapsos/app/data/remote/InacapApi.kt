package com.inacapsos.app.data.remote

import com.inacapsos.app.data.remote.dto.ComentarioDto
import com.inacapsos.app.data.remote.dto.CreateGuardRequestDto
import com.inacapsos.app.data.remote.dto.IncidenteDto
import com.inacapsos.app.data.remote.dto.LoginRequestDto
import com.inacapsos.app.data.remote.dto.LoginResponseDto
import com.inacapsos.app.data.remote.dto.RegisterRequestDto
import com.inacapsos.app.data.remote.dto.ReportDto
import com.inacapsos.app.data.remote.dto.UpdateUserDto
import com.inacapsos.app.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface InacapApi {

    @POST("usuarios/login")
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    @POST("usuarios/register")
    suspend fun register(@Body request: RegisterRequestDto)

    @GET("usuarios/{id}")
    suspend fun getUserDetails(@Path("id") userId: String): UserDto
    
    @GET("usuarios")
    suspend fun getUsers(): List<UserDto>

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

    @PUT("incidente/{id}")
    suspend fun updateIncidente(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<Void>

    @POST("incidente/{id}/comentarios")
    suspend fun addComentario(@Path("id") id: String, @Body body: Map<String, String>): Response<ComentarioDto>

    @GET("incidente/{id}/comentarios")
    suspend fun getComentarios(@Path("id") id: String): List<ComentarioDto>
}
