package com.inacapsos.app.data.remote

import com.inacapsos.app.core.AppSession
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Cambia esta URL por la de tu API real (por ejemplo http://10.0.2.2:3000)
    private const val BASE_URL = "https://inacapsos-api-604122993897.southamerica-west1.run.app"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()

        val token = AppSession.token
        if (token.isNullOrEmpty()){
            android.util.Log.e("API_DEBUG", "⚠️ ALERTA: Se está intentando hacer una petición SIN TOKEN. Por eso falla con 401.")
        } else {
            android.util.Log.d("API_DEBUG", "✅ TOKEN ENVIADO: Bearer $token")
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: InacapApi = retrofit.create(InacapApi::class.java)
}
