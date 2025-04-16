package es.upm.btb.madproject

data class Tweet(
    val userId: String = "",
    val userName: String = "",
    val message: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0
)
