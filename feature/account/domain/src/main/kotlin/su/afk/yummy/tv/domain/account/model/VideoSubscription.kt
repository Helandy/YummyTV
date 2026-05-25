package su.afk.yummy.tv.domain.account.model

data class VideoSubscription(
    val animeId: Int,
    val playerId: Int?,
    val player: String,
    val dubbing: String,
    val posterUrl: String?,
    val title: String,
)
