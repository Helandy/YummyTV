package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.player.common.PlayerEndPromptState
import su.afk.yummy.tv.feature.player.common.isVisible
import su.afk.yummy.tv.feature.player.model.TvPlayerFocusRequesters
import su.afk.yummy.tv.feature.player.model.TvPlayerPromptsState
import su.afk.yummy.tv.feature.player.presentation.R

/** Промпты по центру: следующий эпизод (с отсчётом) и оценка тайтла. */
@Composable
internal fun BoxScope.TvPlayerEndPrompts(
    prompts: TvPlayerPromptsState,
    focus: TvPlayerFocusRequesters,
    hasNextEpisode: Boolean,
    nextEpisodeDubbing: String?,
    onPlayNextEpisode: () -> Unit,
    onRateTitle: () -> Unit,
    onInteraction: () -> Unit,
) {
    TvPlayerEndPrompt(
        visible = prompts.nextEpisodePrompt.isVisible &&
                (hasNextEpisode || nextEpisodeDubbing != null),
        title = when (val prompt = prompts.nextEpisodePrompt) {
            is PlayerEndPromptState.WithCountdown -> stringResource(
                R.string.player_next_episode_prompt_countdown,
                prompt.seconds,
            )

            else -> if (!hasNextEpisode && nextEpisodeDubbing != null) {
                stringResource(
                    R.string.player_next_episode_prompt_other_dubbing,
                    nextEpisodeDubbing,
                )
            } else {
                stringResource(R.string.player_next_episode_prompt)
            }
        },
        primaryLabel = stringResource(R.string.player_watch_next),
        stayLabel = stringResource(R.string.player_stay),
        primaryFocusRequester = focus.nextEpisode,
        onPrimary = onPlayNextEpisode,
        onStay = {
            prompts.nextEpisodePrompt = PlayerEndPromptState.Hidden
            onInteraction()
        },
        onInteraction = onInteraction,
        modifier = Modifier.align(Alignment.Center),
    )

    TvPlayerEndPrompt(
        visible = prompts.showRateTitlePrompt,
        title = stringResource(R.string.player_rate_title_prompt),
        primaryLabel = stringResource(R.string.player_rate_title),
        stayLabel = stringResource(R.string.player_stay),
        primaryFocusRequester = focus.rateTitle,
        onPrimary = onRateTitle,
        onStay = {
            prompts.showRateTitlePrompt = false
            onInteraction()
        },
        onInteraction = onInteraction,
        modifier = Modifier.align(Alignment.Center),
    )
}
