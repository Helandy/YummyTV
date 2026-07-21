package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.foundation.focusable
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
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.kodikThumbnailIframeUrl
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.episodes.utils.watchStatus
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.utils.isAlloha
import su.afk.yummy.tv.feature.player.isKodikPlayerUrl

@Composable
internal fun EpisodesGrid(
    videos: List<AnimeVideo>,
    watchProgress: DetailsWatchProgressIndex,
    restoreFocusRequest: Int,
    onVideoSelected: (AnimeVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Best dubbing by total views among kodik sources
    val bestDubbing = remember(videos) {
        val source =
            videos.filter { it.iframeUrl.isKodikPlayerUrl() }.ifEmpty { videos }
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
    val gridState =
        rememberLazyGridState(initialFirstVisibleItemIndex = (lastFocusedIndex + 1).coerceAtLeast(0))
    val gridFocusRequester = remember { FocusRequester() }
    val focusRequesters = remember(episodeKeys) { List(episodeGroups.size) { FocusRequester() } }
    val scope = rememberCoroutineScope()
    var gridHasFocus by remember { mutableStateOf(false) }
    var isRestoringFocus by remember { mutableStateOf(false) }

    fun requestEpisodeFocus(index: Int, scrollToEpisode: Boolean = true) {
        if (episodeGroups.isEmpty()) return
        val target = index.coerceIn(0, episodeGroups.lastIndex)
        lastFocusedIndex = target
        isRestoringFocus = true
        scope.launch {
            try {
                val gridIndex = target + 1
                if (scrollToEpisode) {
                    gridState.scrollToItem(gridIndex)
                    snapshotFlow { gridState.layoutInfo.visibleItemsInfo.any { it.index == gridIndex } }
                        .first { it }
                } else {
                    withFrameNanos { }
                }
                runCatching { focusRequesters[target].requestFocus() }
            } finally {
                isRestoringFocus = false
            }
        }
    }

    LaunchedEffect(restoreFocusRequest) {
        if (restoreFocusRequest > 0) {
            withFrameNanos { }
            requestEpisodeFocus(lastFocusedIndex, scrollToEpisode = false)
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 220.dp),
        modifier = modifier
            .focusRequester(gridFocusRequester)
            .tvFocusRestorer(
                fallback = focusRequesters.getOrNull(lastFocusedIndex) ?: FocusRequester.Default,
                enabled = episodeGroups.isNotEmpty(),
            )
            .onFocusChanged { state ->
                val hadFocus = gridHasFocus
                gridHasFocus = state.hasFocus
                if (!state.hasFocus) {
                    isRestoringFocus = false
                }
                if (state.isFocused && !hadFocus && episodeGroups.isNotEmpty() && !isRestoringFocus) {
                    requestEpisodeFocus(lastFocusedIndex)
                }
            }
            .focusable(),
        contentPadding = PaddingValues(
            start = TvScreenPadding.Horizontal,
            top = TvScreenPadding.Vertical,
            end = TvScreenPadding.Horizontal,
            bottom = TvScreenPadding.Vertical,
        ),
        horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal),
        verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, contentType = { "header" }) {
            Text(
                text = stringResource(R.string.details_episodes_count_title, episodeGroups.size),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
            )
        }

        itemsIndexed(
            episodeGroups,
            key = { _, entry -> entry.key },
            contentType = { _, _ -> "item" },
        ) { index, (_, groupVideos) ->
            val representative = groupVideos.firstOrNull { it.dubbing == bestDubbing }
                ?: groupVideos.first()
            EpisodeCard(
                video = representative,
                watchStatus = groupVideos.watchStatus(watchProgress),
                kodikIframeUrl = groupVideos.kodikThumbnailIframeUrl(bestDubbing),
                onClick = {
                    lastFocusedIndex = index
                    val kodikOpts = groupVideos.filter {
                        it.iframeUrl.isKodikPlayerUrl() && !it.isAlloha()
                    }
                    val pick = (kodikOpts.firstOrNull { it.dubbing == bestDubbing }
                        ?: kodikOpts.firstOrNull())
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
