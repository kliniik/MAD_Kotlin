package es.upm.btb.madproject

data class Comment(
    val userId: String = "",
    val userName: String = "",
    val message: String = "",
    val timestamp: Long = 0
)