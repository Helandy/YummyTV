package su.afk.yummy.tv.feature.player.common.utils

import androidx.compose.runtime.Composable
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
