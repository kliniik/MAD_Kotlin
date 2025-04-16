package es.upm.btb.madproject.network

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("descripcion") val description: String?,
    @SerializedName("estado") val state: Int?,
    @SerializedName("datos") val dataUrl: String?
)