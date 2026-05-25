package su.afk.yummy.tv.domain.account

data class RemoteWatchState(
    val videoId: Int,
    val timeSeconds: Int,
    val durationSeconds: Int,
)
