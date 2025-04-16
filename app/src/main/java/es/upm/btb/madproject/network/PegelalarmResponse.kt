package es.upm.btb.madproject.network

data class PegelalarmResponse(
    val status: Status,
    val payload: Payload
)

data class Status(
    val code: Int
)

data class Payload(
    val stations: List<Station>
)

data class Station(
    val name: String,
    val commonid: String,
    val latitude: Double,
    val longitude: Double,
    val defaultWarnValueCm: Double,
    val defaultAlarmValueCm: Double,
    val data: List<WaterLevel>
)

data class WaterLevel(
    val type: String,
    val value: Double
)