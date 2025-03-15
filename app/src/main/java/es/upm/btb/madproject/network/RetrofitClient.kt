package es.upm.btb.madproject.network

import android.content.Context
import es.upm.btb.madproject.utils.PreferencesManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val preferencesManager: PreferencesManager by lazy {
        PreferencesManager.getInstance()
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val apiKey = preferencesManager.getApiKey()  // Call API key
            val newRequest = chain.request().newBuilder()
                .addHeader("Authorization", apiKey)  // Set header with API key
                .build()
            chain.proceed(newRequest)
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.pegelalarm.at/api/station/1.0/a/lara_gerlach/")  // Ersetze mit deiner API-BaseURL
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    val api: PegelalarmApiService = retrofit.create(PegelalarmApiService::class.java)
}