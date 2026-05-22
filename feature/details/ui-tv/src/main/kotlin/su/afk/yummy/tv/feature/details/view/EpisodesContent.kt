package su.afk.yummy.tv.feature.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.focusRestorerItem
import su.afk.yummy.tv.core.designsystem.presenter.focus.rememberFocusRestorerState
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.anime.AnimeVideo
import su.afk.yummy.tv.feature.details.R

internal sealed interface EpisodeWatchStatus {
    data object None : EpisodeWatchStatus
    data class InProgress(val progress: Float) : EpisodeWatchStatus
    data object Watched : EpisodeWatchStatus
}

private fun bestWatchStatus(groupVideos: List<AnimeVideo>, watchProgress: Map<String, WatchProgressEntry>): EpisodeWatchStatus {
    val best = groupVideos.mapNotNull { watchProgress[it.iframeUrl] }
        .maxByOrNull { it.positionMs } ?: return EpisodeWatchStatus.None
    if (best.durationMs <= 0 || best.positionMs < 30_000) return EpisodeWatchStatus.None
    val progress = best.positionMs.toFloat() / best.durationMs
    return if (progress > 0.90f) EpisodeWatchStatus.Watched
    else EpisodeWatchStatus.InProgress(progress)
}

@Composable
internal fun EpisodesContent(
    videos: List<AnimeVideo>,
    watchProgress: Map<String, WatchProgressEntry>,
    onVideoSelected: (AnimeVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val kodikIframeByEpisode = remember(videos) {
        videos
            .filter { it.iframeUrl.contains("kodik", ignoreCase = true) }
            .groupBy { it.episode }
            .mapValues { (_, v) -> v.first().iframeUrl }
    }

    // Best dubbing by total views among kodik sources
    val bestDubbing = remember(videos) {
        val source = videos.filter { it.iframeUrl.contains("kodik", ignoreCase = true) }.ifEmpty { videos }
        source.groupBy { it.dubbing }
            .maxByOrNull { (_, list) -> list.sumOf { it.views ?: 0 } }
            ?.key ?: source.firstOrNull()?.dubbing ?: ""
    }

    val episodeGroups = remember(videos) {
        videos
            .groupBy { it.episode }
            .entries
            .sortedBy { it.key.toIntOrNull() ?: 0 }
    }

    val episodesRestorerState = rememberFocusRestorerState()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = TvScreenPadding.Horizontal,
            top = TvScreenPadding.Vertical,
            end = TvScreenPadding.Horizontal,
            bottom = TvScreenPadding.Vertical,
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.details_episodes),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
            )
        }

        itemsIndexed(episodeGroups, key = { _, entry -> entry.key }) { index, (_, groupVideos) ->
            val representative = groupVideos.firstOrNull { it.dubbing == bestDubbing }
                ?: groupVideos.first()
            EpisodeCard(
                video = representative,
                watchStatus = bestWatchStatus(groupVideos, watchProgress),
                kodikIframeUrl = kodikIframeByEpisode[representative.episode],
                onClick = {
                    val kodikOpts = groupVideos.filter {
                        it.iframeUrl.contains("kodik", ignoreCase = true) && !it.isAlloha()
                    }
                    val pick = (kodikOpts.firstOrNull { it.dubbing == bestDubbing } ?: kodikOpts.firstOrNull())
                        ?: groupVideos.firstOrNull { !it.isAlloha() }
                        ?: groupVideos.first()
                    onVideoSelected(pick)
                },
                modifier = Modifier.focusRestorerItem(index, episodesRestorerState),
            )
        }
    }
}
