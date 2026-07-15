package su.afk.yummy.tv.feature.player.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import su.afk.yummy.tv.feature.player.common.PlayerEndPromptState
import su.afk.yummy.tv.feature.player.common.isVisible

/**
 * Промпты конца эпизода: следующий эпизод (сбрасывается по episodeKey/streamUrl)
 * и оценка тайтла (живёт без ключей — как раньше).
 */
@Stable
internal class TvPlayerPromptsState(
    nextEpisodePromptState: MutableState<PlayerEndPromptState>,
    showRateTitlePromptState: MutableState<Boolean>,
) {
    var nextEpisodePrompt: PlayerEndPromptState by nextEpisodePromptState
    var showRateTitlePrompt: Boolean by showRateTitlePromptState

    val anyVisible: Boolean
        get() = nextEpisodePrompt.isVisible || showRateTitlePrompt
}

@Composable
internal fun rememberTvPlayerPromptsState(
    episodeKey: String,
    streamUrl: String,
): TvPlayerPromptsState {
    val nextEpisodePrompt = remember(episodeKey, streamUrl) {
        mutableStateOf<PlayerEndPromptState>(PlayerEndPromptState.Hidden)
    }
    val showRateTitlePrompt = remember { mutableStateOf(false) }
    return remember(nextEpisodePrompt) {
        TvPlayerPromptsState(
            nextEpisodePromptState = nextEpisodePrompt,
            showRateTitlePromptState = showRateTitlePrompt,
        )
    }
}
