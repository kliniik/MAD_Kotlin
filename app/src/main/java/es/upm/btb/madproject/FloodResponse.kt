package es.upm.btb.madproject.network

data class FloodResponse(
    val daily: DailyData
)

data class DailyData(
    val time: List<String>,
    val river_discharge: List<Double>
)