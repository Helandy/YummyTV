package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.focus.rememberTvGridStartExtent
import su.afk.yummy.tv.core.designsystem.focus.rememberTvTopAnchoredGridBringIntoViewSpec
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.focus.tvLazyGridRowFocusNavigation
import su.afk.yummy.tv.core.designsystem.focus.tvWholeItemBringIntoView
import su.afk.yummy.tv.core.model.anime.AnimeEpisodeInfo
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.utils.kodik.kodikThumbnailIframeUrl
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.episodes.utils.watchStatus
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.utils.isAlloha
import su.afk.yummy.tv.feature.player.isKodikPlayerUrl

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EpisodesGrid(
    episodeGroups: List<EpisodesState.EpisodeGroup>,
    bestDubbing: String,
    watchProgress: DetailsWatchProgressIndex,
    restoreFocusRequest: Int,
    episodeInfo: Map<String, AnimeEpisodeInfo>,
    onVideoSelected: (AnimeVideo) -> Unit,
    onVideoLongPressed: (List<AnimeVideo>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val episodeKeys = remember(episodeGroups) { episodeGroups.map { it.episode } }
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
                }
                requestFocusUntilTimeout(focusRequesters[target])
            } finally {
                isRestoringFocus = false
            }
        }
    }

    LaunchedEffect(restoreFocusRequest) {
        if (restoreFocusRequest > 0) {
            requestEpisodeFocus(lastFocusedIndex, scrollToEpisode = false)
        }
    }

    // Панель имеет смысл только когда у тайтла вообще есть данные серий из API
    val showEpisodeInfoPanel = remember(episodeInfo) {
        episodeInfo.values.any { !it.title.isNullOrBlank() || !it.description.isNullOrBlank() }
    }

    Column(modifier = modifier) {
        if (showEpisodeInfoPanel) {
            val focusedEpisode = episodeGroups.getOrNull(lastFocusedIndex)?.episode
            EpisodeInfoPanel(
                episode = focusedEpisode.orEmpty(),
                info = focusedEpisode?.let(episodeInfo::get),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = TvScreenPadding.Horizontal,
                        vertical = TvScreenPadding.Vertical,
                    ),
            )
        }

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val horizontalSpacing = TvCardSpacing.Horizontal
            val gridColumnCount =
                (((maxWidth - TvScreenPadding.Horizontal - TvScreenPadding.Horizontal).value + horizontalSpacing.value) /
                    (EpisodeCardMinWidth.value + horizontalSpacing.value)).toInt()
                    .coerceAtLeast(1)
            // Верхний отступ уже дала панель описания
            val gridTopPadding = if (showEpisodeInfoPanel) 8.dp else TvScreenPadding.Vertical
            val gridStartExtent = rememberTvGridStartExtent(gridTopPadding, TvCardSpacing.Vertical)

            CompositionLocalProvider(
                LocalBringIntoViewSpec provides rememberTvTopAnchoredGridBringIntoViewSpec(TvCardSpacing.Vertical),
            ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = EpisodeCardMinWidth),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(gridFocusRequester)
                        .tvFocusRestorer(
                            fallback = focusRequesters.getOrNull(lastFocusedIndex)
                                ?: FocusRequester.Default,
                            enabled = episodeGroups.isNotEmpty(),
                        )
                        .onFocusChanged { state ->
                            val hadFocus = gridHasFocus
                            gridHasFocus = state.hasFocus
                            if (!state.hasFocus) {
                                isRestoringFocus = false
                            }
                            if (state.isFocused && !hadFocus && episodeGroups.isNotEmpty() &&
                                !isRestoringFocus
                            ) {
                                requestEpisodeFocus(lastFocusedIndex)
                            }
                        }
                        .focusable(),
                    contentPadding = PaddingValues(
                        start = TvScreenPadding.Horizontal,
                        top = gridTopPadding,
                        end = TvScreenPadding.Horizontal,
                        bottom = TvScreenPadding.Vertical,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal),
                    verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }, contentType = { "header" }) {
                        Text(
                            text = stringResource(
                                R.string.details_episodes_count_title,
                                episodeGroups.size,
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = gridStartExtent.measure
                                .fillMaxWidth()
                                .padding(bottom = 2.dp),
                        )
                    }

                    itemsIndexed(
                        episodeGroups,
                        key = { _, entry -> entry.episode },
                        contentType = { _, _ -> "item" },
                    ) { index, (episode, groupVideos) ->
                        val representative = groupVideos.firstOrNull { it.dubbing == bestDubbing }
                            ?: groupVideos.first()
                        EpisodeCard(
                            video = representative,
                            episodeNumber = episode,
                            watchStatus = groupVideos.watchStatus(watchProgress),
                            episodeTitle = episodeInfo[episode]?.title,
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
                            onLongClick = {
                                lastFocusedIndex = index
                                onVideoLongPressed(groupVideos)
                            },
                            modifier = Modifier
                                .focusRequester(focusRequesters[index])
                                .tvWholeItemBringIntoView(gridStartExtent.takeIf { index < gridColumnCount })
                                .tvLazyGridRowFocusNavigation(
                                    index = index,
                                    columnCount = gridColumnCount,
                                    itemCount = episodeGroups.size,
                                    gridState = gridState,
                                    scope = scope,
                                    focusRequesterAt = focusRequesters::getOrNull,
                                    // нулевой lazy-индекс занимает заголовок с числом серий
                                    lazyIndexOffset = 1,
                                )
                                .onFocusChanged {
                                    if (it.hasFocus && !isRestoringFocus) {
                                        lastFocusedIndex = index
                                    }
                                },
                        )
                    }
                }
            }
        }
    }
}

private val EpisodeCardMinWidth = 220.dp
