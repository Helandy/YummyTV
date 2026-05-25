package su.afk.yummy.tv.feature.details.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
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
    restoreFocusRequest: Int,
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

    val episodeKeys = remember(episodeGroups) { episodeGroups.map { it.key } }
    var lastFocusedIndex by rememberSaveable { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = (lastFocusedIndex + 1).coerceAtLeast(0))
    val focusRequesters = remember(episodeKeys) { List(episodeGroups.size) { FocusRequester() } }
    val scope = rememberCoroutineScope()
    var gridHasFocus by remember { mutableStateOf(false) }
    var isRestoringFocus by remember { mutableStateOf(false) }

    fun requestEpisodeFocus(index: Int) {
        if (episodeGroups.isEmpty()) return
        val target = index.coerceIn(0, episodeGroups.lastIndex)
        lastFocusedIndex = target
        isRestoringFocus = true
        scope.launch {
            val gridIndex = target + 1
            gridState.scrollToItem(gridIndex)
            snapshotFlow { gridState.layoutInfo.visibleItemsInfo.any { it.index == gridIndex } }
                .first { it }
            runCatching { focusRequesters[target].requestFocus() }
            isRestoringFocus = false
        }
    }

    LaunchedEffect(restoreFocusRequest) {
        if (restoreFocusRequest > 0) {
            withFrameNanos { }
            requestEpisodeFocus(lastFocusedIndex)
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 220.dp),
        modifier = modifier
            .onFocusChanged { state ->
                val hadFocus = gridHasFocus
                gridHasFocus = state.hasFocus
                if (!state.hasFocus) isRestoringFocus = false
                if (state.hasFocus && !hadFocus && episodeGroups.isNotEmpty()) {
                    requestEpisodeFocus(lastFocusedIndex)
                }
            }
            .focusGroup(),
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
                modifier = Modifier
                    .focusRequester(focusRequesters[index])
                    .onFocusChanged {
                        if (it.hasFocus && !isRestoringFocus) {
                            lastFocusedIndex = index
                        }
                    },
            )
        }
    }
}
