package es.upm.btb.madproject.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // API only valid until 15 July 2025
    private const val BASE_URL = "https://api.pegelalarm.at/api/station/1.0/a/lara_gerlach/"

    val api: PegelalarmApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PegelalarmApiService::class.java)
    }
}