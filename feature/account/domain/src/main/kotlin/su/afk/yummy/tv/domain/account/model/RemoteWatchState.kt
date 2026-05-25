package su.afk.yummy.tv.domain.account.model

data class RemoteWatchState(
    val videoId: Int,
    val timeSeconds: Int,
    val durationSeconds: Int,
)
