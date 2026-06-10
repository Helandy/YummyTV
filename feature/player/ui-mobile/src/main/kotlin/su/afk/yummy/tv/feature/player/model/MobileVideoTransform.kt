package su.afk.yummy.tv.feature.player.model

import androidx.compose.ui.geometry.Offset

internal data class MobileVideoTransform(
    val scale: Float,
    val offset: Offset,
) {
    companion object {
        val Default = MobileVideoTransform(
            scale = 1f,
            offset = Offset.Zero,
        )
    }
}
