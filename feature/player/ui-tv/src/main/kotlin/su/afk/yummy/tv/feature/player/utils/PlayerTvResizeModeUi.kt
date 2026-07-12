package su.afk.yummy.tv.feature.player.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeMode
import su.afk.yummy.tv.core.preferences.settings.PlayerZoomLevel
import su.afk.yummy.tv.feature.player.presentation.R

internal fun tvPlayerContentScale(
    resizeMode: PlayerResizeMode,
    zoomLevel: PlayerZoomLevel,
): ContentScale {
    val zoom = if (resizeMode == PlayerResizeMode.ZOOM) {
        1f + zoomLevel.percent / 100f
    } else {
        1f
    }
    return object : ContentScale {
        override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
            val fit = ContentScale.Fit.computeScaleFactor(srcSize, dstSize)
            return ScaleFactor(fit.scaleX * zoom, fit.scaleY * zoom)
        }
    }
}

@Composable
internal fun PlayerResizeMode.tvResizeLabel(): String = when (this) {
    PlayerResizeMode.FIT -> stringResource(R.string.player_resize_fit)
    PlayerResizeMode.ZOOM -> stringResource(R.string.player_resize_zoom)
}

@Composable
internal fun PlayerResizeMode.tvResizeMeta(): String = when (this) {
    PlayerResizeMode.FIT -> stringResource(R.string.player_resize_fit_meta)
    PlayerResizeMode.ZOOM -> stringResource(R.string.player_resize_zoom_meta)
}

@Composable
internal fun PlayerZoomLevel.tvZoomLevelLabel(): String =
    stringResource(R.string.player_zoom_level_percent, percent)
