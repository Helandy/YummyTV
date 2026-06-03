package su.afk.yummy.tv.feature.details.episodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.details.VideosUiState

@Composable
internal fun EpisodesSection(
    state: VideosUiState,
    watchProgress: Map<String, WatchProgressEntry>,
    restoreFocusRequest: Int,
    onVideoSelected: (AnimeVideo) -> Unit,
) {
    when (state) {
        VideosUiState.Loading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .width(20.dp)
                        .height(20.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = stringResource(R.string.details_loading_episodes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        VideosUiState.Empty -> Unit
        is VideosUiState.Content -> EpisodesContent(
            videos = state.videos,
            watchProgress = watchProgress,
            restoreFocusRequest = restoreFocusRequest,
            onVideoSelected = onVideoSelected,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
