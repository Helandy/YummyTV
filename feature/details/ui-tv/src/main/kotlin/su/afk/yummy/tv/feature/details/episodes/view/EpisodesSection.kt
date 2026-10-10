package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.tv.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.tv.TvStateMessage
import su.afk.yummy.tv.core.model.anime.AnimeEpisodeInfo
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.presentation.R

@Composable
internal fun EpisodesSection(
    state: VideosUiState,
    episodeGroups: List<EpisodesState.EpisodeGroup>,
    bestDubbing: String,
    watchProgress: DetailsWatchProgressIndex,
    restoreFocusRequest: Int,
    onVideoSelected: (AnimeVideo) -> Unit,
    onVideoLongPressed: (List<AnimeVideo>) -> Unit,
    episodeInfo: Map<String, AnimeEpisodeInfo> = emptyMap(),
    onRetry: (() -> Unit)? = null,
) {
    when (state) {
        VideosUiState.Loading -> TvLoadingScreen()

        VideosUiState.NotLoaded -> Unit
        VideosUiState.Empty -> TvStateMessage(
            title = stringResource(R.string.details_episodes_empty),
            icon = Icons.Filled.PlayArrow,
        )

        is VideosUiState.Error -> TvStateMessage(
            title = state.message ?: stringResource(R.string.details_episodes_empty),
            icon = Icons.Filled.Warning,
            onRetry = onRetry,
        )

        is VideosUiState.Content -> EpisodesGrid(
            episodeGroups = episodeGroups,
            bestDubbing = bestDubbing,
            watchProgress = watchProgress,
            restoreFocusRequest = restoreFocusRequest,
            episodeInfo = episodeInfo,
            onVideoSelected = onVideoSelected,
            onVideoLongPressed = onVideoLongPressed,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
