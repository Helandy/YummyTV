package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedItem

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun HomeDashboard(
    feed: HomeFeed,
    continueWatching: List<HomeContinueWatchingItem>,
    onContinueWatchingSelected: (HomeContinueWatchingItem) -> Unit,
    onContinueWatchingFocused: (HomeContinueWatchingItem) -> Unit,
    onItemSelected: (sectionId: String, item: HomeFeedItem) -> Unit,
    onItemFocused: (sectionId: String, displayId: Int, animeId: Int?) -> Unit,
    restoreFocusedItemOnEnter: Boolean = false,
    focusedSectionId: String? = null,
    focusedItemId: Int?,
    continueWatchingRestoreToken: Int,
    continueWatchingRestoreKey: String?,
    onFocusedItemRestoreHandled: () -> Unit,
) {
    val lazyColumnState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current

    val hasContinueWatching = continueWatching.isNotEmpty()
    val hasHero = feed.heroItems.isNotEmpty()

    // LazyColumn item indices used for row-level focus restoration.
    val heroLazyIdx = if (hasContinueWatching) 1 else 0
    val sectionsBaseLazyIdx = heroLazyIdx + if (hasHero) 1 else 0
    val totalLazyItems = sectionsBaseLazyIdx + feed.sections.size
    fun sectionKey(index: Int): String =
        feed.sections.getOrNull(index)?.let { "section_${it.type.name}" } ?: "section_$index"

    // When returning to Home (e.g. back from details/collection) or stepping back down from
    // the top bar, the column regains focus. Prefer the last focused row index because the
    // same anime id can appear in several sections, then fall back to item lookup if the feed
    // changed and that row no longer contains the focused item.
    fun rowIndexForFocusedItem(): Int = when {
        focusedSectionId == SECTION_HERO && hasHero -> heroLazyIdx
        focusedSectionId == SECTION_CONTINUE_WATCHING && hasContinueWatching -> 0
        focusedSectionId != null -> {
            val sectionIdx = feed.sections.indices.indexOfFirst { index ->
                sectionKey(index) == focusedSectionId
            }
            if (sectionIdx >= 0) sectionsBaseLazyIdx + sectionIdx else 0
        }
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
    var restoringFromMainMenu by remember { mutableStateOf(false) }
    var lastFocusedLazyIndex by rememberSaveable { mutableStateOf(if (hasContinueWatching) 0 else if (hasHero) heroLazyIdx else 0) }
    var handledContinueWatchingRestoreToken by rememberSaveable { mutableIntStateOf(0) }
    val homeContentFocusRequester = remember { FocusRequester() }
    val continueWatchingFocusRequester = remember { FocusRequester() }
    val heroFocusRequester = remember { FocusRequester() }
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val sectionFocusRequesters = remember(feed.sections.size) {
        List(feed.sections.size) { FocusRequester() }
    }

    fun focusRequesterForLazyIndex(index: Int): FocusRequester = when {
        hasContinueWatching && index == 0 -> continueWatchingFocusRequester
        hasHero && index == heroLazyIdx -> heroFocusRequester
        index >= sectionsBaseLazyIdx -> {
            val sectionIndex = index - sectionsBaseLazyIdx
            val section = feed.sections.getOrNull(sectionIndex)
            section?.let { sectionFocusRequesters.getOrNull(sectionIndex) }
                ?: firstAvailableFocusRequester(
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

    fun lazyIndexContainsFocusedItem(index: Int): Boolean = when {
        focusedItemId == null -> false
        focusedSectionId == SECTION_HERO -> hasHero && index == heroLazyIdx
        focusedSectionId == SECTION_CONTINUE_WATCHING -> hasContinueWatching && index == 0
        focusedSectionId != null && index >= sectionsBaseLazyIdx -> {
            val sectionIndex = index - sectionsBaseLazyIdx
            sectionIndex in feed.sections.indices && sectionKey(sectionIndex) == focusedSectionId
        }
        hasHero && index == heroLazyIdx -> feed.heroItems.any { it.id == focusedItemId }
        index >= sectionsBaseLazyIdx -> {
            val sectionIndex = index - sectionsBaseLazyIdx
            feed.sections.getOrNull(sectionIndex)?.items?.any { it.id == focusedItemId } == true
        }
        else -> false
    }

    fun focusedLazyIndex(): Int {
        val savedIndex = lastFocusedLazyIndex.coerceIn(0, (totalLazyItems - 1).coerceAtLeast(0))
        return when {
            totalLazyItems <= 0 -> 0
            hasPendingContinueWatchingRestore(
                hasContinueWatching = hasContinueWatching,
                restoreToken = continueWatchingRestoreToken,
                handledToken = handledContinueWatchingRestoreToken,
            ) -> 0
            focusedItemId != null && lazyIndexContainsFocusedItem(savedIndex) -> savedIndex
            focusedItemId != null -> rowIndexForFocusedItem()
            else -> savedIndex
        }
    }

    fun mainMenuEnterIndex(): Int =
        if (focusedSectionId != null || focusedItemId != null) {
            focusedLazyIndex()
        } else if (hasContinueWatching) {
            0
        } else {
            focusedLazyIndex()
        }

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
        direction == FocusDirection.Right -> focusRequesterForLazyIndex(mainMenuEnterIndex())
        else -> focusRequesterForLazyIndex(focusedLazyIndex())
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

    fun consumeContinueWatchingRestoreIfPending(): Boolean {
        if (!hasPendingContinueWatchingRestore(
                hasContinueWatching = hasContinueWatching,
                restoreToken = continueWatchingRestoreToken,
                handledToken = handledContinueWatchingRestoreToken,
            )
        ) {
            return false
        }
        handledContinueWatchingRestoreToken = continueWatchingRestoreToken
        lastFocusedLazyIndex = 0
        return true
    }

    val preferredContentFocusRequester = if (totalLazyItems > 0) {
        val preferredIndex = if (restoreFocusedItemOnEnter) {
            focusedLazyIndex()
        } else {
            mainMenuEnterIndex()
        }
        focusRequesterForLazyIndex(preferredIndex)
    } else {
        null
    }

    DisposableEffect(preferredContentFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(preferredContentFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    LazyColumn(
        state = lazyColumnState,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(homeContentFocusRequester)
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 12.dp)
            .focusProperties {
                mainMenuFocusRequester?.let { left = it }
                onEnter = {
                    restoringFromMainMenu = requestedFocusDirection == FocusDirection.Right
                    if (consumeContinueWatchingRestoreIfPending()) {
                        requestRowFocus(0)
                    } else if (restoringFromMainMenu) {
                        requestRowFocus(mainMenuEnterIndex())
                    } else {
                        restoreFocusRequester(requestedFocusDirection).requestFocus()
                    }
                }
            }
            .onFocusChanged { state ->
                val hadFocus = columnHasFocus
                columnHasFocus = state.hasFocus
                if (state.hasFocus && !hadFocus && totalLazyItems > 0) {
                    val target = when {
                        consumeContinueWatchingRestoreIfPending() -> 0
                        restoringFromMainMenu -> mainMenuEnterIndex()
                        focusedItemId != null -> focusedLazyIndex()
                        else -> lastFocusedLazyIndex.coerceIn(0, totalLazyItems - 1)
                    }
                    scope.launch {
                        lazyColumnState.scrollToItem(target)
                        snapshotFlow {
                            lazyColumnState.layoutInfo.visibleItemsInfo.any { it.index == target }
                        }.first { it }
                        runCatching { focusRequesterForLazyIndex(target).requestFocus() }
                        restoringFromMainMenu = false
                        if (restoreFocusedItemOnEnter) {
                            onFocusedItemRestoreHandled()
                        }
                    }
                } else if (!state.hasFocus) {
                    restoringFromMainMenu = false
                }
            }
            .focusGroup(),
        contentPadding = PaddingValues(bottom = 520.dp),
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
                        onItemFocused = onContinueWatchingFocused,
                        rowFocusRequester = continueWatchingFocusRequester,
                        restoreFirstItemToken = continueWatchingRestoreToken,
                        restoreItemKey = continueWatchingRestoreKey,
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
                            withFrameNanos { }
                        }
                    }
                }

                // Re-scroll when CW section appears/disappears while hero is already focused.
                LaunchedEffect(heroLazyIdx, heroHasFocus, focusedItemId) {
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
                        sectionKey = SECTION_HERO,
                        focusedSectionId = focusedSectionId,
                        focusedItemId = focusedItemId,
                        rowFocusRequester = heroFocusRequester,
                        rowIsFocused = columnHasFocus && lastFocusedLazyIndex == heroLazyIdx,
                        restoreFocusedPageOnEnter = restoreFocusedItemOnEnter && focusedSectionId == SECTION_HERO,
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

        itemsIndexed(
            feed.sections,
            key = { index, _ -> sectionKey(index) },
            contentType = { _, _ -> "section" }) { index, section ->
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
                    focusedSectionId = focusedSectionId,
                    focusedItemId = focusedItemId,
                    rowFocusRequester = sectionFocusRequesters[index],
                    rowIsFocused = columnHasFocus && lastFocusedLazyIndex == lazyIdx,
                    rowKey = sectionKey(index),
                    upFocusRequester = previousRowFocusRequester(lazyIdx),
                    downFocusRequester = nextRowFocusRequester(lazyIdx),
                    bottomPadding = if (index == feed.sections.lastIndex) 96.dp else 20.dp,
                    focusedCardScale = 1f,
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
    sectionFocusRequesters: List<FocusRequester>,
): FocusRequester = when {
    hasContinueWatching -> continueWatchingFocusRequester
    hasHero -> heroFocusRequester
    else -> sectionFocusRequesters.firstOrNull() ?: FocusRequester.Default
}

private const val SECTION_CONTINUE_WATCHING = "__continue_watching"
private const val SECTION_HERO = "__hero"

private fun hasPendingContinueWatchingRestore(
    hasContinueWatching: Boolean,
    restoreToken: Int,
    handledToken: Int,
): Boolean = hasContinueWatching && restoreToken > handledToken
