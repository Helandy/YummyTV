package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.anime.AnimePreview
import su.afk.yummy.tv.domain.home.HomeFeed
import su.afk.yummy.tv.domain.home.HomeFeedItem

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun HomeContent(
    feed: HomeFeed,
    continueWatching: List<WatchProgressEntry>,
    onContinueWatchingSelected: (WatchProgressEntry) -> Unit,
    onItemSelected: (HomeFeedItem) -> Unit,
    onItemFocused: (displayId: Int, animeId: Int?) -> Unit,
    onHeroItemVisible: (displayId: Int) -> Unit,
    focusedItemId: Int?,
    focusedPreview: AnimePreview?,
    animePreviews: Map<Int, AnimePreview>,
) {
    val lazyColumnState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val hasContinueWatching = continueWatching.isNotEmpty()
    val hasHero = feed.heroItems.isNotEmpty()

    // LazyColumn item indices (needed for animateScrollToItem)
    val heroLazyIdx = if (hasContinueWatching) 1 else 0
    val sectionsBaseLazyIdx = heroLazyIdx + if (hasHero) 1 else 0
    val totalLazyItems = sectionsBaseLazyIdx + feed.sections.size

    // When returning to Home (e.g. back from details/collection) or stepping back down from
    // the top bar, the column regains focus. Restore focus to the row that contained the last
    // focused item. The target is derived from focusedItemId, read at the moment the column
    // gains focus: the column's onFocusChanged fires before any card re-focus (parent first)
    // and the forward's onItemFocused is async, so focusedItemId is still the pre-navigation
    // card and can't be clobbered by the restore traversal.
    fun rowIndexForFocusedItem(): Int = when {
        focusedItemId != null && hasHero && feed.heroItems.any { it.id == focusedItemId } -> heroLazyIdx
        focusedItemId != null -> {
            val sectionIdx = feed.sections.indexOfFirst { section ->
                section.items.any { it.id == focusedItemId }
            }
            if (sectionIdx >= 0) sectionsBaseLazyIdx + sectionIdx else 0
        }
        else -> if (hasHero) heroLazyIdx else 0
    }.coerceIn(0, (totalLazyItems - 1).coerceAtLeast(0))

    var columnHasFocus by remember { mutableStateOf(false) }
    var restoringFromTopBar by remember { mutableStateOf(false) }
    var lastFocusedLazyIndex by rememberSaveable { mutableStateOf(if (hasContinueWatching) 0 else if (hasHero) heroLazyIdx else 0) }
    val continueWatchingFocusRequester = remember { FocusRequester() }
    val heroFocusRequester = remember { FocusRequester() }
    val sectionFocusRequesters = remember(feed.sections.map { it.title }) {
        feed.sections.associate { section -> section.title to FocusRequester() }
    }

    fun focusRequesterForFocusedItem(): FocusRequester = when {
        focusedItemId != null && hasHero && feed.heroItems.any { it.id == focusedItemId } -> heroFocusRequester
        focusedItemId != null -> {
            val section = feed.sections.firstOrNull { section ->
                section.items.any { it.id == focusedItemId }
            }
            section?.let { sectionFocusRequesters[it.title] } ?: firstAvailableFocusRequester(
                hasHero = hasHero,
                hasContinueWatching = hasContinueWatching,
                continueWatchingFocusRequester = continueWatchingFocusRequester,
                heroFocusRequester = heroFocusRequester,
                sectionFocusRequesters = sectionFocusRequesters,
            )
        }
        else -> firstAvailableFocusRequester(
            hasHero = hasHero,
            hasContinueWatching = hasContinueWatching,
            continueWatchingFocusRequester = continueWatchingFocusRequester,
            heroFocusRequester = heroFocusRequester,
            sectionFocusRequesters = sectionFocusRequesters,
        )
    }

    fun focusRequesterForLazyIndex(index: Int): FocusRequester = when {
        hasContinueWatching && index == 0 -> continueWatchingFocusRequester
        hasHero && index == heroLazyIdx -> heroFocusRequester
        index >= sectionsBaseLazyIdx -> {
            val sectionIndex = index - sectionsBaseLazyIdx
            val section = feed.sections.getOrNull(sectionIndex)
            section?.let { sectionFocusRequesters[it.title] } ?: firstAvailableFocusRequester(
                hasHero = hasHero,
                hasContinueWatching = hasContinueWatching,
                continueWatchingFocusRequester = continueWatchingFocusRequester,
                heroFocusRequester = heroFocusRequester,
                sectionFocusRequesters = sectionFocusRequesters,
            )
        }
        else -> firstAvailableFocusRequester(
            hasHero = hasHero,
            hasContinueWatching = hasContinueWatching,
            continueWatchingFocusRequester = continueWatchingFocusRequester,
            heroFocusRequester = heroFocusRequester,
            sectionFocusRequesters = sectionFocusRequesters,
        )
    }

    fun topBarEnterIndex(): Int = if (hasContinueWatching) 0 else rowIndexForFocusedItem()

    fun previousRowFocusRequester(index: Int): FocusRequester? =
        when {
            index <= 0 -> null
            else -> focusRequesterForLazyIndex(index - 1)
        }

    fun nextRowFocusRequester(index: Int): FocusRequester? =
        when {
            totalLazyItems <= 0 || index >= totalLazyItems - 1 -> FocusRequester.Cancel
            else -> focusRequesterForLazyIndex(index + 1)
        }

    fun restoreFocusRequester(direction: FocusDirection): FocusRequester = when {
        totalLazyItems <= 0 -> FocusRequester.Default
        direction == FocusDirection.Down -> focusRequesterForLazyIndex(topBarEnterIndex())
        focusedItemId != null -> focusRequesterForFocusedItem()
        else -> focusRequesterForLazyIndex(lastFocusedLazyIndex.coerceIn(0, totalLazyItems - 1))
    }

    fun requestRowFocus(index: Int) {
        if (totalLazyItems <= 0 || index !in 0 until totalLazyItems) return
        val target = index.coerceIn(0, totalLazyItems - 1)
        lastFocusedLazyIndex = target
        scope.launch {
            lazyColumnState.scrollToItem(target)
            snapshotFlow {
                lazyColumnState.layoutInfo.visibleItemsInfo.any { it.index == target }
            }.first { it }
            runCatching { focusRequesterForLazyIndex(target).requestFocus() }
        }
    }

    LazyColumn(
        state = lazyColumnState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusProperties {
                onEnter = {
                    restoringFromTopBar = requestedFocusDirection == FocusDirection.Down
                    restoreFocusRequester(requestedFocusDirection).requestFocus()
                }
            }
            .onFocusChanged { state ->
                val hadFocus = columnHasFocus
                columnHasFocus = state.hasFocus
                if (state.hasFocus && !hadFocus && totalLazyItems > 0) {
                    val target = when {
                        restoringFromTopBar -> topBarEnterIndex()
                        focusedItemId != null -> rowIndexForFocusedItem()
                        else -> lastFocusedLazyIndex.coerceIn(0, totalLazyItems - 1)
                    }
                    scope.launch {
                        lazyColumnState.scrollToItem(target)
                        snapshotFlow {
                            lazyColumnState.layoutInfo.visibleItemsInfo.any { it.index == target }
                        }.first { it }
                        val focusRequester = if (focusedItemId != null) {
                            if (restoringFromTopBar) focusRequesterForLazyIndex(target) else focusRequesterForFocusedItem()
                        } else {
                            focusRequesterForLazyIndex(target)
                        }
                        runCatching { focusRequester.requestFocus() }
                        restoringFromTopBar = false
                    }
                } else if (!state.hasFocus) {
                    restoringFromTopBar = false
                }
            }
            .focusGroup(),
        contentPadding = PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (hasContinueWatching) {
            item(key = "continue_watching") {
                Box(
                    modifier = Modifier.onFocusChanged { state ->
                        if (state.hasFocus) lastFocusedLazyIndex = 0
                    },
                ) {
                    ContinueWatchingSection(
                        items = continueWatching,
                        onItemSelected = onContinueWatchingSelected,
                        rowFocusRequester = continueWatchingFocusRequester,
                        downFocusRequester = nextRowFocusRequester(0),
                        onMoveDown = if (totalLazyItems > 1) {
                            { requestRowFocus(1) }
                        } else {
                            null
                        },
                    )
                }
            }
        }

        if (hasHero) {
            item(key = "hero_carousel") {
                var heroHasFocus by remember { mutableStateOf(false) }
                var heroPinJob by remember { mutableStateOf<Job?>(null) }

                fun pinHeroWhileFocused() {
                    if (!heroHasFocus) return
                    heroPinJob?.cancel()
                    heroPinJob = scope.launch {
                        repeat(4) {
                            if (!heroHasFocus) return@launch
                            lazyColumnState.scrollToItem(heroLazyIdx)
                            delay(80)
                        }
                    }
                }

                // Re-scroll when CW section appears/disappears while hero is already focused
                LaunchedEffect(heroLazyIdx, heroHasFocus, focusedItemId, focusedPreview, animePreviews.size) {
                    pinHeroWhileFocused()
                }

                Box(
                    modifier = Modifier.onFocusChanged { state ->
                        heroHasFocus = state.hasFocus
                        if (state.hasFocus) {
                            lastFocusedLazyIndex = heroLazyIdx
                            pinHeroWhileFocused()
                        } else {
                            heroPinJob?.cancel()
                        }
                    },
                ) {
                    HomeCarousel(
                        items = feed.heroItems,
                        onItemSelected = onItemSelected,
                        onItemFocused = onItemFocused,
                        onItemVisible = onHeroItemVisible,
                        focusedItemId = focusedItemId,
                        focusedPreview = focusedPreview,
                        animePreviews = animePreviews,
                        rowFocusRequester = heroFocusRequester,
                        upFocusRequester = previousRowFocusRequester(heroLazyIdx),
                        downFocusRequester = nextRowFocusRequester(heroLazyIdx),
                        onCarouselFocused = {
                            lastFocusedLazyIndex = heroLazyIdx
                            pinHeroWhileFocused()
                        },
                        onMoveUp = if (heroLazyIdx > 0) {
                            { requestRowFocus(heroLazyIdx - 1) }
                        } else {
                            null
                        },
                        onMoveDown = if (heroLazyIdx < totalLazyItems - 1) {
                            { requestRowFocus(heroLazyIdx + 1) }
                        } else {
                            null
                        },
                    )
                }
            }
        }

        itemsIndexed(feed.sections, key = { _, s -> s.title }, contentType = { _, _ -> "section" }) { index, section ->
            val lazyIdx = sectionsBaseLazyIdx + index
            Box(
                modifier = Modifier.onFocusChanged { state ->
                    if (state.hasFocus) {
                        lastFocusedLazyIndex = lazyIdx
                    }
                },
            ) {
                HomeSection(
                    title = section.title,
                    items = section.items,
                    onItemSelected = onItemSelected,
                    onItemFocused = onItemFocused,
                    focusedItemId = focusedItemId,
                    focusedPreview = focusedPreview,
                    rowFocusRequester = sectionFocusRequesters.getValue(section.title),
                    upFocusRequester = previousRowFocusRequester(lazyIdx),
                    downFocusRequester = nextRowFocusRequester(lazyIdx),
                    bottomPadding = if (index == feed.sections.lastIndex) 56.dp else 20.dp,
                    onMoveUp = if (lazyIdx > 0) {
                        { requestRowFocus(lazyIdx - 1) }
                    } else {
                        null
                    },
                    onMoveDown = if (lazyIdx < totalLazyItems - 1) {
                        { requestRowFocus(lazyIdx + 1) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

private fun firstAvailableFocusRequester(
    hasHero: Boolean,
    hasContinueWatching: Boolean,
    continueWatchingFocusRequester: FocusRequester,
    heroFocusRequester: FocusRequester,
    sectionFocusRequesters: Map<String, FocusRequester>,
): FocusRequester = when {
    hasContinueWatching -> continueWatchingFocusRequester
    hasHero -> heroFocusRequester
    else -> sectionFocusRequesters.values.firstOrNull() ?: FocusRequester.Default
}
