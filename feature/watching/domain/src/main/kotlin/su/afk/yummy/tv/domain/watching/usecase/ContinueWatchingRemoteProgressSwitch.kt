package su.afk.yummy.tv.domain.watching.usecase

data class ContinueWatchingRemoteProgressSwitch(
    val episode: String,
    val positionMs: Long,
)
