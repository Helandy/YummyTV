package su.afk.yummy.tv.feature.player.utils

import android.view.Gravity
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeMode
import su.afk.yummy.tv.core.preferences.settings.PlayerZoomLevel
import su.afk.yummy.tv.feature.player.presentation.R
import androidx.media3.ui.R as Media3R

internal fun PlayerView.applyTvResizeMode(
    resizeMode: PlayerResizeMode,
    zoomLevel: PlayerZoomLevel,
) {
    this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    clipChildren = true
    clipToPadding = true

    val contentFrame = findViewById<AspectRatioFrameLayout>(Media3R.id.exo_content_frame) ?: return
    val params = contentFrame.layoutParams as? FrameLayout.LayoutParams ?: return

    var needsLayout = false
    if (params.gravity != Gravity.CENTER) {
        params.gravity = Gravity.CENTER
        contentFrame.layoutParams = params
        needsLayout = true
    }

    val targetScale = if (resizeMode == PlayerResizeMode.ZOOM) {
        1f + zoomLevel.percent / 100f
    } else {
        1f
    }
    if (contentFrame.translationX != 0f) contentFrame.translationX = 0f
    if (contentFrame.translationY != 0f) contentFrame.translationY = 0f
    if (contentFrame.scaleX != targetScale) contentFrame.scaleX = targetScale
    if (contentFrame.scaleY != targetScale) contentFrame.scaleY = targetScale
    if (needsLayout) contentFrame.requestLayout()
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
