package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.presenter.focus.launchTvLazyGridItemFocusRestore
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.feature.library.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ContinueWatchingGrid(
    entries: List<WatchProgressEntry>,
    focusedItemId: Int?,
    focusedPreview: AnimePreview?,
    gridFocusRequester: FocusRequester,
    selectedTabFocusRequester: FocusRequester,
    restoreFirstItemToken: Int,
    focusStateKey: String,
    onEntrySelected: (WatchProgressEntry) -> Unit,
    onItemFocused: (Int) -> Unit,
    onRemoveWatchProgress: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val entryIds = remember(entries) { entries.map { it.animeId } }
    val focusRequesters = remember(entryIds) { List(entries.size) { FocusRequester() } }
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val cardWidth = currentTvTitleCardDimensions().width
    var lastFocusedIndex by rememberSaveable(focusStateKey) {
        mutableIntStateOf(focusedItemId?.let(entryIds::indexOf)?.takeIf { it >= 0 } ?: 0)
    }
    var lastFocusedEntryId by rememberSaveable(focusStateKey) {
        mutableStateOf(focusedItemId?.takeIf { it in entryIds })
    }
    var gridHasFocus by remember { mutableStateOf(false) }
    var restoringFromMainMenu by remember { mutableStateOf(false) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var pendingFocusAfterDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var pendingDeletedEntryId by remember { mutableStateOf<Int?>(null) }
    var leftEdgeIndexes by remember { mutableStateOf(emptySet<Int>()) }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }

    fun currentIndexFor(entryId: Int?): Int? =
        entryId?.let(entryIds::indexOf)?.takeIf { it >= 0 }

    fun rememberFocusedEntry(index: Int) {
        lastFocusedIndex = index
        lastFocusedEntryId = entryIds.getOrNull(index)
    }

    fun restoreTargetIndex(): Int {
        if (entries.isEmpty()) return 0
        return (
                currentIndexFor(focusedItemId)
                    ?: currentIndexFor(lastFocusedEntryId)
                    ?: lastFocusedIndex
                ).coerceIn(0, entries.lastIndex)
    }

    fun requestEntryFocus(
        index: Int,
        fallbackFocusRequester: FocusRequester = gridFocusRequester,
        clearMainMenuRestore: Boolean = true,
    ) {
        if (entries.isEmpty()) return
        val target = index.coerceIn(0, entries.lastIndex)
        rememberFocusedEntry(target)
        isRestoringFocus = true
        restoreFocusJob = launchTvLazyGridItemFocusRestore(
            previousJob = restoreFocusJob,
            scope = scope,
            itemIndex = target,
            gridState = gridState,
            itemFocusRequesters = focusRequesters,
            fallbackFocusRequester = fallbackFocusRequester,
            onRestoreFinished = {
                isRestoringFocus = false
                if (clearMainMenuRestore) {
                    restoringFromMainMenu = false
                }
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose { restoreFocusJob?.cancel() }
    }

    LaunchedEffect(gridState, entries.size) {
        snapshotFlow {
            val visibleItems = gridState.layoutInfo.visibleItemsInfo
            val minX = visibleItems.minOfOrNull { it.offset.x }
            if (minX == null) {
                emptySet()
            } else {
                visibleItems.filter { it.offset.x == minX }.map { it.index }.toSet()
            }
        }.collect { leftEdgeIndexes = it }
    }

    LaunchedEffect(focusedItemId, entries) {
        currentIndexFor(focusedItemId)?.let { focusedIndex ->
            rememberFocusedEntry(focusedIndex)
        }
    }

    LaunchedEffect(restoreFirstItemToken, entries) {
        if (restoreFirstItemToken <= 0 || entries.isEmpty()) return@LaunchedEffect
        requestEntryFocus(
            index = 0,
            fallbackFocusRequester = selectedTabFocusRequester,
            clearMainMenuRestore = false,
        )
    }

    LaunchedEffect(entries, pendingFocusAfterDeleteIndex, pendingDeletedEntryId) {
        val pendingIndex = pendingFocusAfterDeleteIndex ?: return@LaunchedEffect
        val deletedEntryId = pendingDeletedEntryId ?: return@LaunchedEffect
        if (entries.any { it.animeId == deletedEntryId }) return@LaunchedEffect
        if (entries.isEmpty()) {
            isRestoringFocus = true
            lastFocusedEntryId = null
            repeat(6) {
                runCatching { selectedTabFocusRequester.requestFocus() }
                withFrameNanos { }
            }
            isRestoringFocus = false
        } else {
            val target = pendingIndex.coerceIn(0, entries.lastIndex)
            requestEntryFocus(
                index = target,
                fallbackFocusRequester = selectedTabFocusRequester,
            )
        }
        pendingFocusAfterDeleteIndex = null
        pendingDeletedEntryId = null
    }

    if (entries.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.library_empty_continue_watching),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalSpacing = TvCardSpacing.Horizontal
        val gridHorizontalPadding = TvScreenPadding.Horizontal + TvScreenPadding.Horizontal
        val gridColumnCount =
            (((maxWidth - gridHorizontalPadding).value + horizontalSpacing.value) /
                    (cardWidth.value + horizontalSpacing.value)).toInt().coerceAtLeast(1)
        val gridSpacingWidth = horizontalSpacing * (gridColumnCount - 1).coerceAtLeast(0)
        val adaptiveCardWidth =
            (maxWidth - gridHorizontalPadding - gridSpacingWidth) / gridColumnCount

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(gridColumnCount),
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(gridFocusRequester)
                .focusProperties {
                    onEnter = {
                        restoringFromMainMenu = requestedFocusDirection == FocusDirection.Right
                        requestEntryFocus(if (restoringFromMainMenu) 0 else restoreTargetIndex())
                    }
                }
                .onFocusChanged { state ->
                    val hadFocus = gridHasFocus
                    gridHasFocus = state.hasFocus
                    if (!state.hasFocus) {
                        isRestoringFocus = false
                        restoringFromMainMenu = false
                    }
                    if (state.hasFocus && !hadFocus && entries.isNotEmpty()) {
                        val target = if (restoringFromMainMenu) 0 else restoreTargetIndex()
                        requestEntryFocus(target)
                    }
                }
                .focusGroup()
                .focusable(),
            contentPadding = PaddingValues(
                start = TvScreenPadding.Horizontal,
                end = TvScreenPadding.Horizontal,
                top = 16.dp,
                bottom = TvScreenPadding.Vertical,
            ),
            verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        ) {
            itemsIndexed(entries, key = { _, entry -> entry.animeId }) { index, entry ->
                val progress =
                    if (entry.durationMs > 0) entry.positionMs.toFloat() / entry.durationMs else 0f
                val episodeLabel = if (entry.episode.isNotBlank()) {
                    stringResource(R.string.library_episode_number, entry.episode)
                } else {
                    stringResource(R.string.library_episode)
                }
                val stableOnClick = remember(entry) { { onEntrySelected(entry) } }
                val stableOnFocused = remember(entry.animeId) { { onItemFocused(entry.animeId) } }
                val stableOnDelete = remember(entry.animeId, index) {
                    {
                        pendingFocusAfterDeleteIndex = index
                        pendingDeletedEntryId = entry.animeId
                        val immediateTarget =
                            if (index < entries.lastIndex) index + 1 else index - 1
                        if (immediateTarget >= 0) {
                            rememberFocusedEntry(immediateTarget)
                            runCatching { focusRequesters[immediateTarget].requestFocus() }
                        } else {
                            lastFocusedEntryId = null
                            runCatching { selectedTabFocusRequester.requestFocus() }
                        }
                        onRemoveWatchProgress(entry.animeId)
                    }
                }
                LibraryAnimeCard(
                    title = entry.animeTitle,
                    posterUrl = entry.posterUrl.ifBlank { null },
                    subtitle = episodeLabel,
                    onClick = stableOnClick,
                    onFocused = stableOnFocused,
                    onDelete = stableOnDelete,
                    cardWidth = adaptiveCardWidth,
                    screenshotUrls = if (entry.animeId == focusedItemId) focusedPreview?.screenshotUrls.orEmpty() else emptyList(),
                    modifier = Modifier
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionLeft) {
                                return@onPreviewKeyEvent false
                            }
                            if (index !in leftEdgeIndexes) {
                                requestEntryFocus(index - 1)
                                true
                            } else {
                                runCatching { mainMenuFocusRequester?.requestFocus() }
                                mainMenuFocusRequester != null
                            }
                        }
                        .onFocusChanged { state ->
                            if (state.hasFocus && gridHasFocus && !isRestoringFocus) {
                                rememberFocusedEntry(index)
                            }
                        }
                        .focusGroup(),
                    cardModifier = Modifier
                        .focusRequester(focusRequesters[index])
                        .onPreviewKeyEvent { event ->
                            if (
                                event.type == KeyEventType.KeyDown &&
                                event.key == Key.DirectionUp &&
                                index < gridColumnCount
                            ) {
                                runCatching { selectedTabFocusRequester.requestFocus() }
                                true
                            } else {
                                false
                            }
                        }
                        .focusProperties {
                            if (index !in leftEdgeIndexes) {
                                focusRequesters.getOrNull(index - 1)?.let { left = it }
                            }
                            focusRequesters.getOrNull(index + 1)?.let { right = it }
                        },
                    deleteModifier = Modifier.focusProperties {
                        if (index !in leftEdgeIndexes) {
                            focusRequesters.getOrNull(index - 1)?.let { left = it }
                        }
                        focusRequesters.getOrNull(index + 1)?.let { right = it }
                        up = focusRequesters[index]
                    },
                    posterOverlay = {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(InProgressColor.copy(alpha = 0.25f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(3.dp)
                                    .background(InProgressColor),
                            )
                        }
                    },
                )
            }
        }
    }
}
