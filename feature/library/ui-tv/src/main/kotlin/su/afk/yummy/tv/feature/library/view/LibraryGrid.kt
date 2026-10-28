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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import su.afk.yummy.tv.core.designsystem.tv.TvStateMessage
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import su.afk.yummy.tv.core.designsystem.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.dimensions.tvPosterGridColumns
import su.afk.yummy.tv.core.designsystem.focus.launchTvLazyGridKeyFocusRestore
import su.afk.yummy.tv.core.designsystem.focus.rememberTvLazyFocusRestoreState
import su.afk.yummy.tv.core.designsystem.focus.TvTopAnchoredExtraRowGap
import su.afk.yummy.tv.core.designsystem.focus.rememberTvTopAnchoredGridBringIntoViewSpec
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.focus.tvLazyGridRowFocusNavigation
import su.afk.yummy.tv.core.designsystem.focus.tvWholeItemBringIntoView
import su.afk.yummy.tv.core.designsystem.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterQuality
import su.afk.yummy.tv.core.designsystem.time.rememberNowEpochSeconds
import su.afk.yummy.tv.core.model.anime.urlsByQuality
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.feature.library.mapper.toTitleListCardUi
import su.afk.yummy.tv.feature.library.model.LibraryTab
import su.afk.yummy.tv.feature.library.model.LibraryTvCardActions
import su.afk.yummy.tv.feature.library.presentation.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryGrid(
    tab: LibraryTab,
    items: List<LibraryItem>,
    gridFocusRequester: FocusRequester,
    selectedTabFocusRequester: FocusRequester,
    focusStateKey: String,
    showTitleYear: Boolean,
    onAnimeSelected: (Int) -> Unit,
    onRemoveEntry: (Int) -> Unit,
    onCardActions: (LibraryTvCardActions) -> Unit,
    /** Спрашивает подтверждение; переданное действие (фокус + удаление) выполняется только после «Удалить». */
    confirmRemoval: (title: String, proceed: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val itemIds = remember(items) { items.map { it.animeId } }
    val focusRequesters = remember(itemIds) { List(items.size) { FocusRequester() } }
    val deleteFocusRequesters = remember(itemIds) { List(items.size) { FocusRequester() } }
    val itemFocusRequesters = remember(itemIds, focusRequesters) {
        itemIds.zip(focusRequesters).toMap()
    }
    val focusRestoreState = rememberTvLazyFocusRestoreState<Int>(focusStateKey)
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val posterQuality = LocalPosterQuality.current
    val cardWidth = LibraryCardPreferredWidth
    val nowEpochSeconds = rememberNowEpochSeconds()
    var gridHasFocus by remember { mutableStateOf(false) }
    var restoringFromMainMenu by remember { mutableStateOf(false) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var pendingFocusAfterDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var pendingDeletedItemId by remember { mutableStateOf<Int?>(null) }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }

    fun rememberFocusedItem(index: Int) {
        itemIds.getOrNull(index)?.let { key ->
            focusRestoreState.onItemFocused(key, index)
        }
    }

    fun restoreTargetIndex(): Int {
        if (items.isEmpty()) return 0
        return focusRestoreState.targetIndex(itemIds)?.coerceIn(0, items.lastIndex) ?: 0
    }

    fun gridFallbackFocusRequester(): FocusRequester =
        focusRequesters.getOrNull(restoreTargetIndex()) ?: selectedTabFocusRequester

    fun requestItemFocus(
        index: Int,
        fallbackFocusRequester: FocusRequester? = null,
        clearMainMenuRestore: Boolean = true,
    ) {
        if (items.isEmpty()) return
        val target = index.coerceIn(0, items.lastIndex)
        rememberFocusedItem(target)
        isRestoringFocus = true
        restoreFocusJob = launchTvLazyGridKeyFocusRestore(
            previousJob = restoreFocusJob,
            scope = scope,
            restoreState = focusRestoreState,
            keys = itemIds,
            gridState = gridState,
            itemFocusRequesters = itemFocusRequesters,
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

    LaunchedEffect(items, pendingFocusAfterDeleteIndex, pendingDeletedItemId) {
        val pendingIndex = pendingFocusAfterDeleteIndex ?: return@LaunchedEffect
        val deletedItemId = pendingDeletedItemId ?: return@LaunchedEffect
        if (items.any { it.animeId == deletedItemId }) return@LaunchedEffect
        if (items.isEmpty()) {
            isRestoringFocus = true
            focusRestoreState.clear()
            requestFocusUntilTimeout(selectedTabFocusRequester)
            isRestoringFocus = false
        } else {
            val target = pendingIndex.coerceIn(0, items.lastIndex)
            requestItemFocus(
                index = target,
                fallbackFocusRequester = selectedTabFocusRequester,
            )
        }
        pendingFocusAfterDeleteIndex = null
        pendingDeletedItemId = null
    }

    if (items.isEmpty()) {
        TvStateMessage(
            title = stringResource(R.string.library_empty_list),
            icon = Icons.AutoMirrored.Filled.List,
            modifier = modifier,
        )
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalSpacing = TvCardSpacing.Horizontal
        val gridHorizontalPadding = TvScreenPadding.Horizontal + TvScreenPadding.Horizontal
        val gridColumnCount = tvPosterGridColumns(
            availableWidth = maxWidth - gridHorizontalPadding,
            cardWidth = cardWidth,
            spacing = horizontalSpacing,
        )

        CompositionLocalProvider(
            LocalBringIntoViewSpec provides rememberTvTopAnchoredGridBringIntoViewSpec(
                rowSpacing = TvCardSpacing.Vertical,
                topContentPadding = LibraryGridTopPadding,
            ),
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(gridColumnCount),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(gridFocusRequester)
                    .tvFocusRestorer(
                        fallback = gridFallbackFocusRequester(),
                        enabled = items.isNotEmpty(),
                    )
                    .onFocusChanged { state ->
                        val hadFocus = gridHasFocus
                        gridHasFocus = state.hasFocus
                        if (!state.hasFocus) {
                            isRestoringFocus = false
                            restoringFromMainMenu = false
                        }
                        if (state.isFocused && !hadFocus && items.isNotEmpty() && !isRestoringFocus) {
                            val target = if (restoringFromMainMenu) 0 else restoreTargetIndex()
                            requestItemFocus(target)
                        }
                    }
                    .focusable(),
                contentPadding = PaddingValues(
                    start = TvScreenPadding.Horizontal,
                    end = TvScreenPadding.Horizontal,
                    top = LibraryGridTopPadding,
                    bottom = TvScreenPadding.Vertical,
                ),
                verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical + TvTopAnchoredExtraRowGap),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            ) {
                itemsIndexed(items, key = { index, item -> lazyKey("library", item.animeId, index) }) { index, item ->
                    val stableOnClick = remember(item.animeId, index) {
                        {
                            rememberFocusedItem(index)
                            onAnimeSelected(item.animeId)
                        }
                    }
                    val stableOnFocused =
                        remember(item.animeId, index) { { rememberFocusedItem(index) } }
                    val posterUrls = item.poster.urlsByQuality(posterQuality)
                    val card = remember(item, tab, posterUrls) { item.toTitleListCardUi(tab, posterUrls) }
                    val stableOnDelete = remember(item.animeId, index) {
                        {
                          confirmRemoval(item.title) {
                            pendingFocusAfterDeleteIndex = index
                            pendingDeletedItemId = item.animeId
                            val immediateTarget =
                                if (index < items.lastIndex) index + 1 else index - 1
                            if (immediateTarget >= 0) {
                                rememberFocusedItem(immediateTarget)
                                runCatching { focusRequesters[immediateTarget].requestFocus() }
                            } else {
                                focusRestoreState.clear()
                                runCatching { selectedTabFocusRequester.requestFocus() }
                            }
                            onRemoveEntry(item.animeId)
                          }
                        }
                    }
                    val stableOnLongClick = remember(item.animeId, index, stableOnClick, stableOnDelete) {
                        {
                            onCardActions(
                                LibraryTvCardActions(
                                    title = item.title,
                                    open = stableOnClick,
                                    remove = stableOnDelete,
                                ),
                            )
                        }
                    }
                    LibraryAnimeCard(
                        item = card,
                        nowEpochSeconds = nowEpochSeconds,
                        showYear = showTitleYear,
                        onClick = stableOnClick,
                        onLongClick = stableOnLongClick,
                        onFocused = stableOnFocused,
                        deleteButton = {
                            LibraryTvDeleteButton(
                                onClick = stableOnDelete,
                                modifier = Modifier
                                    .focusRequester(deleteFocusRequesters[index])
                                    .focusProperties {
                                        left = focusRequesters[index]
                                        // Колонка крестиков: без явных цепочек поиск уходит на карточку ряда ниже.
                                        if (index < gridColumnCount) {
                                            up = selectedTabFocusRequester
                                        } else {
                                            deleteFocusRequesters.getOrNull(index - gridColumnCount)?.let { up = it }
                                        }
                                        deleteFocusRequesters.getOrNull(index + gridColumnCount)?.let { down = it }
                                    }
                                    // Вертикаль — по крестикам, чтобы колонка не прыгала на карточки.
                                    .tvLazyGridRowFocusNavigation(
                                        index = index,
                                        columnCount = gridColumnCount,
                                        itemCount = items.size,
                                        gridState = gridState,
                                        scope = scope,
                                        focusRequesterAt = deleteFocusRequesters::getOrNull,
                                    ),
                            )
                        },
                        modifier = Modifier
                            .tvWholeItemBringIntoView()
                            .onFocusChanged { state ->
                                if (state.hasFocus && gridHasFocus && !isRestoringFocus) {
                                    rememberFocusedItem(index)
                                }
                            }
                            .focusRequester(focusRequesters[index])
                            .focusProperties {
                                if (index % gridColumnCount == 0) {
                                    mainMenuFocusRequester?.let { left = it }
                                }
                                if (index < gridColumnCount) {
                                    up = selectedTabFocusRequester
                                }
                                right = deleteFocusRequesters[index]
                            }
                            // Карточка — единственная цель фокуса в ячейке: колонку при DPAD-вниз
                            // страхуем здесь, иначе Compose теряет её на несобранном ряду.
                            .tvLazyGridRowFocusNavigation(
                                index = index,
                                columnCount = gridColumnCount,
                                itemCount = items.size,
                                gridState = gridState,
                                scope = scope,
                                focusRequesterAt = focusRequesters::getOrNull,
                            ),
                    )
                }
            }
        }
    }
}

/** Верхний отступ сетки; тот же отступ передаётся в якорь прокрутки, иначе из-за кромки торчит низ предыдущего ряда. */
private val LibraryGridTopPadding = 8.dp
