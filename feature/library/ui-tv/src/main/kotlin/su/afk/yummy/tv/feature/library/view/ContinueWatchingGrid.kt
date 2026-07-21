package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.TvFocusedGridBringIntoViewSpec
import su.afk.yummy.tv.core.designsystem.presenter.focus.launchTvLazyGridKeyFocusRestore
import su.afk.yummy.tv.core.designsystem.presenter.focus.rememberTvLazyFocusRestoreState
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.preferences.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.feature.library.R
import su.afk.yummy.tv.feature.library.utils.continueWatchingFocusKey

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContinueWatchingGrid(
    entries: List<HomeContinueWatchingItem>,
    cardSize: LibraryContinueWatchingCardSize,
    gridFocusRequester: FocusRequester,
    selectedTabFocusRequester: FocusRequester,
    focusStateKey: String,
    onEntrySelected: (HomeContinueWatchingItem) -> Unit,
    onDetailsSelected: (HomeContinueWatchingItem) -> Unit,
    onRemoveWatchProgress: (HomeContinueWatchingItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val entryKeys = remember(entries) { entries.map { it.continueWatchingFocusKey() } }
    val focusRequesters = remember(entryKeys) { List(entries.size) { FocusRequester() } }
    val detailsFocusRequesters = remember(entryKeys) { List(entries.size) { FocusRequester() } }
    val deleteFocusRequesters = remember(entryKeys) { List(entries.size) { FocusRequester() } }
    val entryFocusRequesters = remember(entryKeys, focusRequesters) {
        entryKeys.zip(focusRequesters).toMap()
    }
    val focusRestoreState = rememberTvLazyFocusRestoreState<String>(focusStateKey)
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val minCardWidth = cardSize.tvMinCardWidth
    var gridHasFocus by remember { mutableStateOf(false) }
    var restoringFromMainMenu by remember { mutableStateOf(false) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var pendingFocusAfterDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var pendingDeletedEntryId by remember { mutableStateOf<Int?>(null) }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }

    fun rememberFocusedEntry(index: Int) {
        entryKeys.getOrNull(index)?.let { key ->
            focusRestoreState.onItemFocused(key, index)
        }
    }

    fun restoreTargetIndex(): Int {
        if (entries.isEmpty()) return 0
        return focusRestoreState.targetIndex(entryKeys)?.coerceIn(0, entries.lastIndex) ?: 0
    }

    fun gridFallbackFocusRequester(): FocusRequester =
        focusRequesters.getOrNull(restoreTargetIndex()) ?: selectedTabFocusRequester

    fun requestEntryFocus(
        index: Int,
        fallbackFocusRequester: FocusRequester? = null,
        clearMainMenuRestore: Boolean = true,
    ) {
        if (entries.isEmpty()) return
        val target = index.coerceIn(0, entries.lastIndex)
        rememberFocusedEntry(target)
        isRestoringFocus = true
        restoreFocusJob = launchTvLazyGridKeyFocusRestore(
            previousJob = restoreFocusJob,
            scope = scope,
            restoreState = focusRestoreState,
            keys = entryKeys,
            gridState = gridState,
            itemFocusRequesters = entryFocusRequesters,
            fallbackFocusRequester = fallbackFocusRequester ?: gridFallbackFocusRequester(),
            fallbackIndex = target,
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

    LaunchedEffect(entries, pendingFocusAfterDeleteIndex, pendingDeletedEntryId) {
        val pendingIndex = pendingFocusAfterDeleteIndex ?: return@LaunchedEffect
        val deletedEntryId = pendingDeletedEntryId ?: return@LaunchedEffect
        if (entries.any { it.animeId == deletedEntryId }) return@LaunchedEffect
        if (entries.isEmpty()) {
            isRestoringFocus = true
            focusRestoreState.clear()
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
                    (minCardWidth.value + horizontalSpacing.value)).toInt().coerceAtLeast(1)
        val gridSpacingWidth = horizontalSpacing * (gridColumnCount - 1).coerceAtLeast(0)
        val adaptiveCardWidth =
            (maxWidth - gridHorizontalPadding - gridSpacingWidth) / gridColumnCount
        CompositionLocalProvider(
            LocalBringIntoViewSpec provides TvFocusedGridBringIntoViewSpec,
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(gridColumnCount),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(gridFocusRequester)
                    .tvFocusRestorer(
                        fallback = gridFallbackFocusRequester(),
                        enabled = entries.isNotEmpty(),
                    )
                    .onFocusChanged { state ->
                        val hadFocus = gridHasFocus
                        gridHasFocus = state.hasFocus
                        if (!state.hasFocus) {
                            isRestoringFocus = false
                            restoringFromMainMenu = false
                        }
                        if (state.isFocused && !hadFocus && entries.isNotEmpty() && !isRestoringFocus) {
                            val target = if (restoringFromMainMenu) 0 else restoreTargetIndex()
                            requestEntryFocus(target)
                        }
                    }
                    .focusable(),
                contentPadding = PaddingValues(
                    start = TvScreenPadding.Horizontal,
                    end = TvScreenPadding.Horizontal,
                    top = 16.dp,
                    bottom = TvScreenPadding.Vertical,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            ) {
                itemsIndexed(
                    entries,
                    key = { _, entry -> entry.continueWatchingFocusKey() }) { index, entry ->
                    val episodeLabel = if (entry.episode.isNotBlank()) {
                        stringResource(R.string.library_episode_number, entry.episode)
                    } else {
                        stringResource(R.string.library_episode)
                    }
                    val stableOnClick = remember(entry, index) {
                        {
                            rememberFocusedEntry(index)
                            onEntrySelected(entry)
                        }
                    }
                    val stableOnDetails = remember(entry, index) {
                        {
                            rememberFocusedEntry(index)
                            onDetailsSelected(entry)
                        }
                    }
                    val stableOnFocused =
                        remember(entry.animeId, index) { { rememberFocusedEntry(index) } }
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
                                focusRestoreState.clear()
                                runCatching { selectedTabFocusRequester.requestFocus() }
                            }
                            onRemoveWatchProgress(entry)
                        }
                    }
                    ContinueWatchingGridCard(
                        entry = entry,
                        episodeLabel = episodeLabel,
                        onClick = stableOnClick,
                        onFocused = stableOnFocused,
                        onDetails = stableOnDetails,
                        onDelete = stableOnDelete,
                        cardWidth = adaptiveCardWidth,
                        modifier = Modifier
                            .onFocusChanged { state ->
                                if (state.hasFocus && gridHasFocus && !isRestoringFocus) {
                                    rememberFocusedEntry(index)
                                }
                            }
                            .tvFocusRestorer(fallback = focusRequesters[index]),
                        cardModifier = Modifier
                            .focusRequester(focusRequesters[index])
                            .focusProperties {
                                if (index % gridColumnCount == 0) {
                                    mainMenuFocusRequester?.let { left = it }
                                }
                                if (index < gridColumnCount) {
                                    up = selectedTabFocusRequester
                                }
                            },
                        detailsModifier = Modifier
                            .focusRequester(detailsFocusRequesters[index])
                            .focusProperties {
                                up = focusRequesters[index]
                                right = deleteFocusRequesters[index]
                                if (index % gridColumnCount == 0) {
                                    mainMenuFocusRequester?.let { left = it }
                                }
                            },
                        deleteModifier = Modifier
                            .focusRequester(deleteFocusRequesters[index])
                            .focusProperties {
                                left = detailsFocusRequesters[index]
                                up = focusRequesters[index]
                            },
                        leftFocusRequester = if (index % gridColumnCount == 0) {
                            mainMenuFocusRequester
                        } else {
                            null
                        },
                        rightFocusRequester = null,
                        upFocusRequester = if (index < gridColumnCount) {
                            selectedTabFocusRequester
                        } else {
                            null
                        },
                        downFocusRequester = detailsFocusRequesters[index],
                    )
                }
            }
        }
    }
}

private val LibraryContinueWatchingCardSize.tvMinCardWidth
    get() = when (this) {
        LibraryContinueWatchingCardSize.COMPACT -> 220.dp
        LibraryContinueWatchingCardSize.STANDARD -> 250.dp
        LibraryContinueWatchingCardSize.LARGE -> 280.dp
    }
