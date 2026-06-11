package su.afk.yummy.tv.core.preferences.settings

data class PlayerResizeSettings(
    val resizeMode: PlayerResizeMode = PlayerResizeMode.FIT,
    val zoomLevel: PlayerZoomLevel = PlayerZoomLevel.PERCENT_10,
)
