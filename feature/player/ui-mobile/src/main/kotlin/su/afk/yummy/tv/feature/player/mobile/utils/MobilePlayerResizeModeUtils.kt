package su.afk.yummy.tv.feature.player.mobile.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.feature.player.presentation.R

/** Порядок кнопок масштаба — тот же, что в меню на ТВ: зум последним. */
internal val mobileResizeModeOrder = listOf(
    PlayerResizeMode.FIT,
    PlayerResizeMode.CROP,
    PlayerResizeMode.STRETCH,
    PlayerResizeMode.ZOOM,
)

internal fun PlayerResizeMode.mobileResizeLabelRes(): Int = when (this) {
    PlayerResizeMode.FIT -> R.string.player_resize_fit
    PlayerResizeMode.ZOOM -> R.string.player_resize_zoom
    PlayerResizeMode.STRETCH -> R.string.player_resize_stretch
    PlayerResizeMode.CROP -> R.string.player_resize_crop
}

internal fun PlayerResizeMode.mobileResizeIcon(): ImageVector = when (this) {
    PlayerResizeMode.FIT -> Icons.Filled.FitScreen
    PlayerResizeMode.ZOOM -> Icons.Filled.ZoomIn
    PlayerResizeMode.STRETCH -> Icons.Filled.OpenInFull
    PlayerResizeMode.CROP -> Icons.Filled.Crop
}
