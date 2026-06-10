package su.afk.yummy.tv.feature.player.utils

import android.view.Gravity
import android.widget.FrameLayout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import su.afk.yummy.tv.feature.player.model.MobileVideoTransform
import androidx.media3.ui.R as Media3R

internal const val MOBILE_PLAYER_MIN_VIDEO_SCALE = 1f
internal const val MOBILE_PLAYER_MAX_VIDEO_SCALE = 3f

internal fun calculateMobileVideoTransform(
    currentScale: Float,
    currentOffset: Offset,
    playerSize: IntSize,
    centroid: Offset,
    pan: Offset,
    zoomChange: Float,
): MobileVideoTransform {
    val newScale = (currentScale * zoomChange).coerceIn(
        MOBILE_PLAYER_MIN_VIDEO_SCALE,
        MOBILE_PLAYER_MAX_VIDEO_SCALE,
    )
    val nextOffset = if (newScale <= MOBILE_PLAYER_MIN_VIDEO_SCALE) {
        Offset.Zero
    } else {
        val center = Offset(playerSize.width / 2f, playerSize.height / 2f)
        val centroidFromCenter = centroid - center
        val scaleChange = newScale / currentScale
        ((currentOffset + centroidFromCenter) * scaleChange) - centroidFromCenter + pan
    }
    return MobileVideoTransform(
        scale = newScale,
        offset = nextOffset.clampedForMobileVideoScale(newScale, playerSize),
    )
}

internal fun PlayerView.applyMobileVideoTransform(
    scale: Float,
    offset: Offset,
) {
    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    clipChildren = true
    clipToPadding = true

    val contentFrame = findViewById<AspectRatioFrameLayout>(Media3R.id.exo_content_frame) ?: return
    val params = contentFrame.layoutParams as? FrameLayout.LayoutParams
    if (params != null && params.gravity != Gravity.CENTER) {
        params.gravity = Gravity.CENTER
        contentFrame.layoutParams = params
    }

    contentFrame.clipChildren = true
    contentFrame.clipToPadding = true
    contentFrame.scaleX = scale
    contentFrame.scaleY = scale
    contentFrame.translationX = offset.x
    contentFrame.translationY = offset.y
}

private fun Offset.clampedForMobileVideoScale(scale: Float, size: IntSize): Offset {
    if (scale <= MOBILE_PLAYER_MIN_VIDEO_SCALE || size == IntSize.Zero) return Offset.Zero

    val maxX = (size.width * (scale - MOBILE_PLAYER_MIN_VIDEO_SCALE) / 2f).coerceAtLeast(0f)
    val maxY = (size.height * (scale - MOBILE_PLAYER_MIN_VIDEO_SCALE) / 2f).coerceAtLeast(0f)
    return Offset(
        x = x.coerceIn(-maxX, maxX),
        y = y.coerceIn(-maxY, maxY),
    )
}
