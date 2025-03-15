package es.upm.btb.madproject.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface PegelalarmApiService {
    @GET("list") // API Endpoint
    fun getWaterLevelData(
        @Query("commonid") commonId: String // `commonid` as Query Parameter
    ): Call<PegelalarmResponse>
}