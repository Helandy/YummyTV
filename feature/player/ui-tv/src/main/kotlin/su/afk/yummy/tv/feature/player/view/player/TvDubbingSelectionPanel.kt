package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.player.common.utils.formatCompactCount
import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState
import su.afk.yummy.tv.feature.player.presentation.R

/**
 * Панель выбора озвучки: просмотры, число серий и балансеры у каждой, недоступные для серии
 * серые. Одна и та же в плеере и в панели ошибки потока.
 */
@Composable
internal fun TvDubbingSelectionPanel(
    visible: Boolean,
    playback: PlayerPlaybackUiState,
    selectedFocusRequester: FocusRequester,
    onItemSelected: (index: Int) -> Unit,
    onExitDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvPlayerSelectionPanel(
        visible = visible,
        title = stringResource(R.string.player_dubbing_title),
        items = playback.dubbingNames,
        selectedIndex = playback.currentDubbingIndex,
        selectedFocusRequester = selectedFocusRequester,
        enabledItems = playback.dubbingAvailability,
        accentLabel = true,
        disabledItemMeta = stringResource(R.string.player_episode_unavailable),
        modifier = modifier,
        itemMetaContent = { idx, contentColor ->
            TvDubbingMetaRow(
                views = playback.dubbingViews.getOrElse(idx) { 0 }.formatCompactCount(),
                episodeCount = playback.dubbingEpisodeCounts.getOrElse(idx) { 0 },
                sourceNames = playback.dubbingSourceNames.getOrElse(idx) { "" },
                contentColor = contentColor,
            )
        },
        onItemSelected = onItemSelected,
        onExitDown = onExitDown,
    )
}
