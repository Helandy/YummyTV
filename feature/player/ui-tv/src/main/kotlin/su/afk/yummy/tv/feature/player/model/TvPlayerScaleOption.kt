package su.afk.yummy.tv.feature.player.model

import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.model.settings.PlayerZoomLevel

/**
 * Пункт подменю «Масштаб» на ТВ: режим масштабирования, а для [PlayerResizeMode.ZOOM] — ещё и
 * уровень увеличения, чтобы режим и уровень выбирались одним списком.
 */
internal data class TvPlayerScaleOption(
    val mode: PlayerResizeMode,
    val zoomLevel: PlayerZoomLevel? = null,
)
