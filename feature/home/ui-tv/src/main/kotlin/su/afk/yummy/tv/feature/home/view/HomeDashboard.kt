package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedItem

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun HomeDashboard(
    feed: HomeFeed,
    continueWatching: List<HomeContinueWatchingItem>,
    onContinueWatchingSelected: (HomeContinueWatchingItem) -> Unit,
    onItemSelected: (sectionId: String, item: HomeFeedItem) -> Unit,
) {
    val lazyColumnState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current

    val hasContinueWatching = continueWatching.isNotEmpty()
    val hasHero = feed.heroItems.isNotEmpty()

    // LazyColumn item indices used for row-level focus restoration.
    val heroLazyIdx = if (hasContinueWatching) 1 else 0
    val sectionsBaseLazyIdx = heroLazyIdx + if (hasHero) 1 else 0
    val totalLazyItems = sectionsBaseLazyIdx + feed.sections.size
    fun sectionKey(index: Int): String =
        feed.sections.getOrNull(index)?.let { "section_${it.type.name}" } ?: "section_$index"

    var columnHasFocus by remember { mutableStateOf(false) }
    var lastFocusedLazyIndex by rememberSaveable { mutableStateOf(if (hasContinueWatching) 0 else if (hasHero) heroLazyIdx else 0) }
    var lastFocusedSectionItemKeys by rememberSaveable {
        mutableStateOf<Map<String, String>>(
            emptyMap()
        )
    }
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

    fun focusedLazyIndex(): Int {
        val savedIndex = lastFocusedLazyIndex.coerceIn(0, (totalLazyItems - 1).coerceAtLeast(0))
        return if (totalLazyItems <= 0) 0 else savedIndex
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

    val preferredContentFocusRequester = focusRequesterForLazyIndex(focusedLazyIndex())

    DisposableEffect(preferredContentFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(preferredContentFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    CompositionLocalProvider(
        LocalBringIntoViewSpec provides HomeColumnNoAutoBringIntoViewSpec,
    ) {
        LazyColumn(
            state = lazyColumnState,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(homeContentFocusRequester)
                .background(MaterialTheme.colorScheme.background)
                .padding(top = 12.dp)
                .focusProperties {
                    mainMenuFocusRequester?.let { left = it }
                }
                .tvFocusRestorer(fallback = focusRequesterForLazyIndex(focusedLazyIndex()))
                .onFocusChanged { state ->
                    columnHasFocus = state.hasFocus
                },
            contentPadding = PaddingValues(bottom = 520.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (hasContinueWatching) {
                item(key = "continue_watching") {
                    CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
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
            }

            if (hasHero) {
                item(key = "hero_carousel") {
                    CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                        var heroRowHasFocus by remember { mutableStateOf(false) }
                        var heroEnterScrollJob by remember { mutableStateOf<Job?>(null) }

                        fun scrollHeroToTopWhileFocused() {
                            heroEnterScrollJob?.cancel()
                            heroEnterScrollJob = scope.launch {
                                repeat(4) {
                                    if (!heroRowHasFocus) return@launch
                                    lazyColumnState.scrollToItem(heroLazyIdx)
                                    withFrameNanos { }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier.onFocusChanged { state ->
                                val hadFocus = heroRowHasFocus
                                heroRowHasFocus = state.hasFocus
                                if (state.hasFocus) {
                                    lastFocusedLazyIndex = heroLazyIdx
                                    if (!hadFocus) {
                                        scrollHeroToTopWhileFocused()
                                    }
                                } else {
                                    heroEnterScrollJob?.cancel()
                                }
                            },
                        ) {
                            HomeCarousel(
                                items = feed.heroItems,
                                onItemSelected = onItemSelected,
                                sectionKey = SECTION_HERO,
                                rowFocusRequester = heroFocusRequester,
                                rowIsFocused = columnHasFocus && lastFocusedLazyIndex == heroLazyIdx,
                                upFocusRequester = previousRowFocusRequester(heroLazyIdx),
                                downFocusRequester = nextRowFocusRequester(heroLazyIdx),
                                onCarouselFocused = {
                                    lastFocusedLazyIndex = heroLazyIdx
                                },
                                onCarouselFocusSettled = {
                                    scrollHeroToTopWhileFocused()
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
            }

            itemsIndexed(
                feed.sections,
                key = { index, _ -> sectionKey(index) },
                contentType = { _, _ -> "section" }) { index, section ->
                CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                    val lazyIdx = sectionsBaseLazyIdx + index
                    val rowKey = sectionKey(index)
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
                            rowFocusRequester = sectionFocusRequesters[index],
                            rowIsFocused = columnHasFocus && lastFocusedLazyIndex == lazyIdx,
                            rowKey = rowKey,
                            restoreItemKey = lastFocusedSectionItemKeys[rowKey],
                            onFocusedItemKeyChanged = { itemKey ->
                                lastFocusedSectionItemKeys =
                                    lastFocusedSectionItemKeys + (rowKey to itemKey)
                            },
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

@OptIn(ExperimentalFoundationApi::class)
private object HomeColumnNoAutoBringIntoViewSpec : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float,
    ): Float = 0f
}

private const val SECTION_HERO = "__hero"
