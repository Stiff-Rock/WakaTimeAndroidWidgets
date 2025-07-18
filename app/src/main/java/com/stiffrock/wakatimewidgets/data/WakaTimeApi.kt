package com.stiffrock.wakatimewidgets.data

import android.util.Base64
import com.stiffrock.wakatimewidgets.data.model.LangStatsResponse
import com.stiffrock.wakatimewidgets.data.model.UserResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header

// API interface
interface WakaTimeApiService {
    @GET("users/current")
    suspend fun getCurrentUser(
        @Header("Authorization") authorization: String
    ): Response<UserResponse>

    @GET("users/current/stats/last_7_days")
    suspend fun getLangStats(
        @Header("Authorization") authorization: String
    ): Response<LangStatsResponse>
}

// API client singleton
object WakaTimeApi {
    private const val BASE_URL = "https://wakatime.com/api/v1/"

    private val retrofit by lazy {
        Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: WakaTimeApiService by lazy {
        retrofit.create(WakaTimeApiService::class.java)
    }

    // Format the API key as required by WakaTime
    fun formatApiKey(apiKey: String): String {
        return "Basic ${
            Base64.encodeToString(
                apiKey.toByteArray(), Base64.NO_WRAP
            )
        }"
    }
}