package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.player.common.PlayerEndFlowState
import su.afk.yummy.tv.feature.player.common.utils.hasOtherSource
import su.afk.yummy.tv.feature.player.common.utils.isVisible
import su.afk.yummy.tv.feature.player.common.utils.playerFinalEpisodePrimaryLabel
import su.afk.yummy.tv.feature.player.common.utils.playerFinalEpisodePromptSubtitle
import su.afk.yummy.tv.feature.player.common.utils.playerFinalEpisodePromptTitle
import su.afk.yummy.tv.feature.player.common.utils.playerNextEpisodePromptTitle
import su.afk.yummy.tv.feature.player.common.utils.playerNextEpisodeUnavailablePrimaryLabel
import su.afk.yummy.tv.feature.player.common.utils.playerNextEpisodeUnavailableSecondaryLabel
import su.afk.yummy.tv.feature.player.common.utils.playerNextEpisodeUnavailableSubtitle
import su.afk.yummy.tv.feature.player.common.utils.playerNextEpisodeUnavailableTitle
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.model.PlayerNextEpisodeSourceOption
import su.afk.yummy.tv.feature.player.model.TvPlayerFocusRequesters
import su.afk.yummy.tv.feature.player.presentation.R

/**
 * Промпты по центру: следующий эпизод и финальное действие тайтла (оба предлагают «Смотреть снова»),
 * а также диалог, что в активной озвучке следующей серии нет.
 */
@Composable
internal fun BoxScope.TvPlayerEndPrompts(
    prompts: PlayerEndFlowState,
    focus: TvPlayerFocusRequesters,
    hasNextEpisode: Boolean,
    activeEpisode: String,
    onPlayNextEpisode: () -> Unit,
    onSelectNextEpisodeSource: (PlayerNextEpisodeSourceOption) -> Unit,
    onCloseNextEpisodeUnavailable: () -> Unit,
    onWatchAgain: () -> Unit,
    onRateTitle: () -> Unit,
    onManageSubscriptions: () -> Unit,
    onInteraction: () -> Unit,
) {
    TvPlayerEndPrompt(
        visible = prompts.nextEpisodePrompt.isVisible && hasNextEpisode,
        title = playerNextEpisodePromptTitle(prompts.nextEpisodePrompt),
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
        title = playerFinalEpisodePromptTitle(finalAction, activeEpisode),
        subtitle = playerFinalEpisodePromptSubtitle(finalAction),
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

    val unavailable = prompts.nextEpisodeUnavailable
    if (unavailable != null && unavailable.hasOtherSource) {
        TvPlayerNextEpisodeSourcesPrompt(
            title = playerNextEpisodeUnavailableTitle(unavailable),
            subtitle = playerNextEpisodeUnavailableSubtitle(unavailable),
            options = unavailable.options,
            firstFocusRequester = focus.nextEpisodeUnavailable,
            onSelect = onSelectNextEpisodeSource,
            onCancel = onCloseNextEpisodeUnavailable,
            onInteraction = onInteraction,
            modifier = Modifier.align(Alignment.Center),
        )
    } else if (unavailable != null) {
        TvPlayerEndPrompt(
            visible = true,
            title = playerNextEpisodeUnavailableTitle(unavailable),
            subtitle = playerNextEpisodeUnavailableSubtitle(unavailable),
            primaryLabel = playerNextEpisodeUnavailablePrimaryLabel(unavailable),
            secondaryLabel = playerNextEpisodeUnavailableSecondaryLabel(unavailable),
            primaryFocusRequester = focus.nextEpisodeUnavailable,
            onPrimary = if (unavailable.canSubscribe) {
                {
                    prompts.nextEpisodeUnavailable = null
                    onManageSubscriptions()
                }
            } else {
                onCloseNextEpisodeUnavailable
            },
            onSecondary = onCloseNextEpisodeUnavailable,
            onInteraction = onInteraction,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
