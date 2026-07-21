package su.afk.yummy.tv.domain.account.model

data class UserWatchHistoryDay(
    val dateSeconds: Long,
    val durationSeconds: Long,
    val episodeCount: Int,
)
