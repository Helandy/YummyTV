package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.player.common.PlayerEndPromptState
import su.afk.yummy.tv.feature.player.common.isVisible
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.model.TvPlayerFocusRequesters
import su.afk.yummy.tv.feature.player.model.TvPlayerPromptsState
import su.afk.yummy.tv.feature.player.presentation.R

/** Промпты по центру: следующий эпизод и финальное действие тайтла. */
@Composable
internal fun BoxScope.TvPlayerEndPrompts(
    prompts: TvPlayerPromptsState,
    focus: TvPlayerFocusRequesters,
    hasNextEpisode: Boolean,
    nextEpisodeDubbing: String?,
    onPlayNextEpisode: () -> Unit,
    onRateTitle: () -> Unit,
    onManageSubscriptions: () -> Unit,
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

    val finalAction = prompts.finalEpisodeActionPrompt
    TvPlayerEndPrompt(
        visible = finalAction != null,
        title = stringResource(
            if (finalAction == PlayerFinalEpisodeAction.ManageSubscriptions) {
                R.string.player_notifications_prompt
            } else {
                R.string.player_rate_title_prompt
            }
        ),
        primaryLabel = stringResource(
            if (finalAction == PlayerFinalEpisodeAction.ManageSubscriptions) {
                R.string.player_manage_notifications
            } else {
                R.string.player_rate_title
            }
        ),
        stayLabel = stringResource(R.string.player_stay),
        primaryFocusRequester = focus.finalEpisodeAction,
        onPrimary = {
            if (finalAction == PlayerFinalEpisodeAction.ManageSubscriptions) {
                onManageSubscriptions()
            } else {
                onRateTitle()
            }
        },
        onStay = {
            prompts.finalEpisodeActionPrompt = null
            onInteraction()
        },
        onInteraction = onInteraction,
        modifier = Modifier.align(Alignment.Center),
    )
}
