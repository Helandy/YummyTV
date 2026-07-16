package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.details.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.details.VideosUiState

@Composable
internal fun EpisodesSection(
    state: VideosUiState,
    watchProgress: DetailsWatchProgressIndex,
    restoreFocusRequest: Int,
    onVideoSelected: (AnimeVideo) -> Unit,
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
            videos = state.videos,
            watchProgress = watchProgress,
            restoreFocusRequest = restoreFocusRequest,
            onVideoSelected = onVideoSelected,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
