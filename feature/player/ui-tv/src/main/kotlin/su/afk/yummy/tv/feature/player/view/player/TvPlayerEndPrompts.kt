package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.player.common.PlayerEndFlowState
import su.afk.yummy.tv.feature.player.common.utils.isVisible
import su.afk.yummy.tv.feature.player.common.utils.playerFinalEpisodePrimaryLabel
import su.afk.yummy.tv.feature.player.common.utils.playerFinalEpisodePromptTitle
import su.afk.yummy.tv.feature.player.common.utils.playerNextEpisodePromptTitle
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.model.TvPlayerFocusRequesters
import su.afk.yummy.tv.feature.player.presentation.R

/** Промпты по центру: следующий эпизод и финальное действие тайтла; оба предлагают «Смотреть снова». */
@Composable
internal fun BoxScope.TvPlayerEndPrompts(
    prompts: PlayerEndFlowState,
    focus: TvPlayerFocusRequesters,
    hasNextEpisode: Boolean,
    nextEpisodeDubbing: String?,
    onPlayNextEpisode: () -> Unit,
    onWatchAgain: () -> Unit,
    onRateTitle: () -> Unit,
    onManageSubscriptions: () -> Unit,
    onInteraction: () -> Unit,
) {
    TvPlayerEndPrompt(
        visible = prompts.nextEpisodePrompt.isVisible &&
                (hasNextEpisode || nextEpisodeDubbing != null),
        title = playerNextEpisodePromptTitle(
            prompt = prompts.nextEpisodePrompt,
            hasNextEpisode = hasNextEpisode,
            nextEpisodeDubbing = nextEpisodeDubbing,
        ),
        primaryLabel = stringResource(R.string.player_watch_next),
        secondaryLabel = stringResource(R.string.player_watch_again),
        primaryFocusRequester = focus.nextEpisode,
        onPrimary = onPlayNextEpisode,
        onSecondary = onWatchAgain,
        onInteraction = onInteraction,
        modifier = Modifier.align(Alignment.Center),
    )

    val finalAction = prompts.finalEpisodeActionPrompt
    TvPlayerEndPrompt(
        visible = finalAction != null,
        title = playerFinalEpisodePromptTitle(finalAction),
        primaryLabel = playerFinalEpisodePrimaryLabel(finalAction),
        secondaryLabel = stringResource(R.string.player_watch_again),
        primaryFocusRequester = focus.finalEpisodeAction,
        onPrimary = {
            if (finalAction == PlayerFinalEpisodeAction.ManageSubscriptions) {
                onManageSubscriptions()
            } else {
                onRateTitle()
            }
        },
        onSecondary = onWatchAgain,
        onInteraction = onInteraction,
        modifier = Modifier.align(Alignment.Center),
    )
}
