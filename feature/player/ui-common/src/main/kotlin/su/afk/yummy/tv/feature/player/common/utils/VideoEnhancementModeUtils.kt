package su.afk.yummy.tv.feature.player.common.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.VideoEnhancementMode
import su.afk.yummy.tv.feature.player.presentation.R

/** Название режима улучшения видео в меню плеера. */
@Composable
fun VideoEnhancementMode.label(): String = stringResource(
    when (this) {
        VideoEnhancementMode.OFF -> R.string.player_toggle_off
        VideoEnhancementMode.SHARPEN -> R.string.player_video_enhancement_sharpen
        VideoEnhancementMode.QUALITY -> R.string.player_video_enhancement_quality
    },
)

/** Иконка режима улучшения видео в меню плеера. */
fun VideoEnhancementMode.icon(): ImageVector = when (this) {
    VideoEnhancementMode.OFF -> Icons.Filled.Block
    VideoEnhancementMode.SHARPEN -> Icons.Filled.AutoFixHigh
    VideoEnhancementMode.QUALITY -> Icons.Filled.HighQuality
}
