package su.afk.yummy.tv.feature.player.mobile.pip.model

internal data class MobilePlayerPipCallbacks(
    val onSeekBackward: () -> Unit,
    val onPlayPause: () -> Unit,
    val onSeekForward: () -> Unit,
)
