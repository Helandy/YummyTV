package su.afk.yummy.tv.feature.player.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.model.settings.PlayerZoomLevel
import su.afk.yummy.tv.feature.player.common.utils.playerContentScale
import su.afk.yummy.tv.feature.player.model.TvPlayerScaleOption
import su.afk.yummy.tv.feature.player.presentation.R

internal fun tvPlayerContentScale(
    resizeMode: PlayerResizeMode,
    zoomLevel: PlayerZoomLevel,
) = playerContentScale(resizeMode, zoomLevel)

@Composable
internal fun PlayerResizeMode.tvResizeLabel(): String = when (this) {
    PlayerResizeMode.FIT -> stringResource(R.string.player_resize_fit)
    PlayerResizeMode.ZOOM -> stringResource(R.string.player_resize_zoom)
    PlayerResizeMode.STRETCH -> stringResource(R.string.player_resize_stretch)
    PlayerResizeMode.CROP -> stringResource(R.string.player_resize_crop)
}

/** Порядок режимов в подменю «Масштаб» на ТВ: зум идёт последним. */
private val tvPlayerScaleModeOrder = listOf(
    PlayerResizeMode.FIT,
    PlayerResizeMode.CROP,
    PlayerResizeMode.STRETCH,
    PlayerResizeMode.ZOOM,
)

/** Варианты подменю «Масштаб»: все режимы, а «Увеличение» развёрнуто по уровням. */
internal val tvPlayerScaleOptions: List<TvPlayerScaleOption> =
    tvPlayerScaleModeOrder.flatMap { mode ->
        if (mode == PlayerResizeMode.ZOOM) {
            PlayerZoomLevel.entries.map { level -> TvPlayerScaleOption(mode, level) }
        } else {
            listOf(TvPlayerScaleOption(mode))
        }
    }

internal fun TvPlayerScaleOption.isSelected(
    resizeMode: PlayerResizeMode,
    zoomLevel: PlayerZoomLevel,
): Boolean = mode == resizeMode && (this.zoomLevel == null || this.zoomLevel == zoomLevel)

@Composable
internal fun TvPlayerScaleOption.tvLabel(): String =
    zoomLevel?.let { "${mode.tvResizeLabel()} +${it.percent}%" } ?: mode.tvResizeLabel()

internal fun TvPlayerScaleOption.tvIcon(): ImageVector = when (mode) {
    PlayerResizeMode.FIT -> Icons.Filled.FitScreen
    PlayerResizeMode.CROP -> Icons.Filled.Crop
    PlayerResizeMode.STRETCH -> Icons.Filled.OpenInFull
    PlayerResizeMode.ZOOM -> Icons.Filled.ZoomIn
}
