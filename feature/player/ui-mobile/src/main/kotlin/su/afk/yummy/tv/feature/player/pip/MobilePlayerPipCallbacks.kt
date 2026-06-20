package su.afk.yummy.tv.feature.player.pip

internal data class MobilePlayerPipCallbacks(
    val onSeekBackward: () -> Unit,
    val onPlayPause: () -> Unit,
    val onSeekForward: () -> Unit,
)
