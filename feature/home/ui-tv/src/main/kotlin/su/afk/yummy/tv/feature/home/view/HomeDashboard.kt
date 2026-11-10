package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.model.settings.HomeBlockKind
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedSection
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import su.afk.yummy.tv.feature.home.presentation.R
import su.afk.yummy.tv.feature.home.utils.toSectionType
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun HomeDashboard(
    feed: HomeFeed,
    blocks: List<HomeBlockKind>,
    continueWatching: List<HomeContinueWatchingItem>,
    launchingContinueWatchingAnimeId: Int?,
    onContinueWatchingSelected: (HomeContinueWatchingItem) -> Unit,
    onItemSelected: (sectionId: String, item: HomeFeedItem) -> Unit,
    requestInitialFocus: Boolean,
    onInitialFocusHandled: () -> Unit,
    onRecommendationLongClick: (HomeFeedItem) -> Unit = {},
) {
    val lazyColumnState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current

    val hasContinueWatching = continueWatching.isNotEmpty()
    val hasHero = feed.heroItems.isNotEmpty()

    // Ряды в порядке настроек. Секции нумеруем по исходному списку, чтобы ключи рядов (а с ними
    // и возврат фокуса) не зависели от порядка блоков.
    val rows = remember(blocks, feed.sections, hasContinueWatching, hasHero) {
        blocks.mapNotNull { kind ->
            when (kind) {
                HomeBlockKind.CONTINUE_WATCHING ->
                    HomeDashboardRow.ContinueWatching.takeIf { hasContinueWatching }

                HomeBlockKind.HERO -> HomeDashboardRow.Hero.takeIf { hasHero }
                else -> kind.toSectionType()
                    ?.let { type -> feed.sections.indexOfFirst { it.type == type } }
                    ?.takeIf { it >= 0 }
                    ?.let { HomeDashboardRow.Section(it) }
            }
        }
    }

    // LazyColumn item indices used for row-level focus restoration.
    val totalLazyItems = rows.size
    fun sectionKey(index: Int): String =
        feed.sections.getOrNull(index)?.let { "section_${it.type.name}" } ?: "section_$index"

    fun rowKey(row: HomeDashboardRow): String = when (row) {
        HomeDashboardRow.ContinueWatching -> ROW_CONTINUE_WATCHING
        HomeDashboardRow.Hero -> ROW_HERO
        is HomeDashboardRow.Section -> sectionKey(row.index)
    }

    /** Позиция секции (её индекс в [HomeFeed.sections]) среди элементов LazyColumn. */
    fun lazyIndexForSection(sectionIndex: Int): Int =
        rows.indexOfFirst { it is HomeDashboardRow.Section && it.index == sectionIndex }

    val continueWatchingLazyIdx = rows.indexOf(HomeDashboardRow.ContinueWatching)
    val heroLazyIdx = rows.indexOf(HomeDashboardRow.Hero)

    var columnHasFocus by remember { mutableStateOf(false) }

    // Ряд запоминается по стабильному ключу, а не по индексу LazyColumn: состав рядов
    // динамический ("Продолжить просмотр" может появиться/исчезнуть между уходом и
    // возвратом), и сохранённый индекс указывал бы уже на другой ряд.
    var lastFocusedRowKey by rememberSaveable { mutableStateOf<String?>(null) }
    var lastFocusedSectionItemKeys by rememberSaveable {
        mutableStateOf<Map<String, String>>(
            emptyMap(),
        )
    }
    val homeContentFocusRequester = remember { FocusRequester() }
    val continueWatchingFocusRequester = remember { FocusRequester() }
    val heroFocusRequester = remember { FocusRequester() }
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val sectionFocusRequesters = remember(feed.sections.size) {
        List(feed.sections.size) { FocusRequester() }
    }

    // FocusRequester ряда прикреплён к конкретной карточке вложенного LazyRow, и после
    // обновления фида карточка восстановления может оказаться вне скомпонованного окна ряда —
    // тогда requestFocus() фейлится до таймаута и фокус «залипает» в текущем ряду. Ряды
    // регистрируют обработчик, который сначала подкручивает вложенный ряд к нужной карточке.
    val rowFocusHandlers = remember { mutableMapOf<String, suspend () -> Boolean>() }
    fun registerRowFocusHandler(key: String, handler: (suspend () -> Boolean)?) {
        if (handler == null) rowFocusHandlers.remove(key) else rowFocusHandlers[key] = handler
    }

    val rowFocusJob = remember { mutableStateOf<Job?>(null) }

    fun focusRequesterForLazyIndex(index: Int): FocusRequester =
        when (val row = rows.getOrNull(index) ?: rows.firstOrNull()) {
            HomeDashboardRow.ContinueWatching -> continueWatchingFocusRequester
            HomeDashboardRow.Hero -> heroFocusRequester
            is HomeDashboardRow.Section -> sectionFocusRequesters[row.index]
            null -> heroFocusRequester
        }

    fun lazyIndexForRowKey(key: String?): Int =
        if (key == null) -1 else rows.indexOfFirst { rowKey(it) == key }

    fun rowKeyForLazyIndex(index: Int): String? = rows.getOrNull(index)?.let(::rowKey)

    fun focusedLazyIndex(): Int {
        val index = lazyIndexForRowKey(lastFocusedRowKey)
        return if (index in 0 until totalLazyItems) index else 0
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
        val previousRowKey = lastFocusedRowKey
        val targetRowKey = rowKeyForLazyIndex(target)
        lastFocusedRowKey = targetRowKey
        // Быстрые повторные нажатия: предыдущий цикл подбора фокуса отменяется, чтобы два
        // конкурирующих цикла не завершились в обратном порядке — побеждает последнее нажатие.
        rowFocusJob.value?.cancel()
        rowFocusJob.value = scope.launch {
            lazyColumnState.scrollToItem(target)
            snapshotFlow {
                lazyColumnState.layoutInfo.visibleItemsInfo.any { it.index == target }
            }.first { it }
            // FocusRequester ряда прикреплён к карточке внутри вложенного LazyRow,
            // которая может быть ещё не скомпонована на этом кадре — повторяем по кадрам
            val focused = requestFocusUntilTimeout(ROW_FOCUS_TIMEOUT) {
                val handler = targetRowKey?.let { rowFocusHandlers[it] }
                handler?.invoke() ?: focusRequesterForLazyIndex(target).requestFocus()
            }
            if (!focused) {
                // Фокус фактически остался в прежнем ряду — возвращаем ключ, иначе подсветка
                // заголовков и fallback восстановления будут указывать не на тот ряд.
                if (lastFocusedRowKey == targetRowKey) {
                    lastFocusedRowKey = previousRowKey
                }
            }
        }
    }

    val preferredContentFocusRequester = focusRequesterForLazyIndex(focusedLazyIndex())

    DisposableEffect(preferredContentFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(preferredContentFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    // Фокус держал лоадер главной — после загрузки переносим его на запомненный ряд
    // (при первом входе — «Продолжить просмотр»), не полагаясь на стартовую попытку скаффолда.
    LaunchedEffect(requestInitialFocus) {
        if (!requestInitialFocus) return@LaunchedEffect
        requestRowFocus(focusedLazyIndex())
        onInitialFocusHandled()
    }

    CompositionLocalProvider(
        LocalBringIntoViewSpec provides HomeColumnNoAutoBringIntoViewSpec,
    ) {
        // Ряд секции эмитится из двух мест, поэтому собран одной функцией.
        fun LazyListScope.homeSectionItem(sectionIndex: Int, section: HomeFeedSection) {
            val rowKey = sectionKey(sectionIndex)
            item(key = rowKey, contentType = "section") {
                CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                    val lazyIdx = lazyIndexForSection(sectionIndex)
                    HomeDashboardSectionRow(
                        section = section,
                        rowKey = rowKey,
                        rowIsFocused = columnHasFocus && lastFocusedRowKey == rowKey,
                        rowFocusRequester = sectionFocusRequesters[sectionIndex],
                        restoreItemKey = lastFocusedSectionItemKeys[rowKey],
                        showYear = section.type == HomeFeedSectionType.RECOMMENDATIONS,
                        bottomPadding = if (lazyIdx == totalLazyItems - 1) 96.dp else 20.dp,
                        upFocusRequester = previousRowFocusRequester(lazyIdx),
                        downFocusRequester = nextRowFocusRequester(lazyIdx),
                        onRowFocused = { justEntered ->
                            lastFocusedRowKey = rowKey
                            // Авто-подскролл колонки отключён (HomeColumnNoAutoBringIntoViewSpec),
                            // поэтому при входе в ряд выравниваем колонку вручную — фокус может
                            // прийти в обход requestRowFocus (focusProperties.up, focus search при
                            // восстановлении после возврата в Home).
                            if (justEntered) scope.launch { lazyColumnState.scrollToItem(lazyIdx) }
                        },
                        registerFocusHandler = { handler ->
                            registerRowFocusHandler(rowKey, handler)
                        },
                        onItemSelected = onItemSelected,
                        // Управлять видимостью можно только рекомендациями.
                        onItemLongClick = onRecommendationLongClick
                            .takeIf { section.type == HomeFeedSectionType.RECOMMENDATIONS },
                        onFocusedItemKeyChanged = { itemKey ->
                            lastFocusedSectionItemKeys =
                                lastFocusedSectionItemKeys + (rowKey to itemKey)
                        },
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

        LazyColumn(
            state = lazyColumnState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_feed")
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
            rows.forEach { row ->
                when (row) {
                    HomeDashboardRow.ContinueWatching ->
                    item(key = ROW_CONTINUE_WATCHING) {
                        CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                            var rowHadFocus by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier.onFocusChanged { state ->
                                    val hadFocus = rowHadFocus
                                    rowHadFocus = state.hasFocus
                                    if (state.hasFocus) {
                                        lastFocusedRowKey = ROW_CONTINUE_WATCHING
                                        if (!hadFocus) {
                                            scope.launch { lazyColumnState.scrollToItem(continueWatchingLazyIdx) }
                                        }
                                    }
                                },
                            ) {
                                Column {
                                    ContinueWatchingSection(
                                        items = continueWatching,
                                        launchingAnimeId = launchingContinueWatchingAnimeId,
                                        onItemSelected = onContinueWatchingSelected,
                                        rowFocusRequester = continueWatchingFocusRequester,
                                        registerFocusHandler = { handler ->
                                            registerRowFocusHandler(ROW_CONTINUE_WATCHING, handler)
                                        },
                                        downFocusRequester = nextRowFocusRequester(continueWatchingLazyIdx),
                                        onMoveDown = if (continueWatchingLazyIdx < totalLazyItems - 1) {
                                            { requestRowFocus(continueWatchingLazyIdx + 1) }
                                        } else {
                                            null
                                        },
                                    )
                                }
                            }
                        }
                    }

                    HomeDashboardRow.Hero ->
                    item(key = ROW_HERO) {
                        CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                            var heroRowHasFocus by remember { mutableStateOf(false) }
                            var heroEnterScrollJob by remember { mutableStateOf<Job?>(null) }

                            fun scrollHeroToTopWhileFocused() {
                                heroEnterScrollJob?.cancel()
                                heroEnterScrollJob = scope.launch {
                                    // Пока фокус в hero, держим его у верхней кромки: реагируем на
                                    // каждое смещение колонки, пока не встанет ровно.
                                    withTimeoutOrNull(ROW_FOCUS_TIMEOUT) {
                                        snapshotFlow {
                                            heroRowHasFocus &&
                                                (lazyColumnState.firstVisibleItemIndex != heroLazyIdx ||
                                                    lazyColumnState.firstVisibleItemScrollOffset != 0)
                                        }.first { needsScroll ->
                                            if (needsScroll) lazyColumnState.scrollToItem(heroLazyIdx)
                                            !needsScroll
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier.onFocusChanged { state ->
                                    val hadFocus = heroRowHasFocus
                                    heroRowHasFocus = state.hasFocus
                                    if (state.hasFocus) {
                                        lastFocusedRowKey = ROW_HERO
                                        if (!hadFocus) {
                                            scrollHeroToTopWhileFocused()
                                        }
                                    } else {
                                        heroEnterScrollJob?.cancel()
                                    }
                                },
                            ) {
                                Column {
                                    HomeSectionHeader(
                                        title = stringResource(R.string.home_season_title),
                                        active = columnHasFocus && lastFocusedRowKey == ROW_HERO,
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    HomeCarousel(
                                        items = feed.heroItems,
                                        onItemSelected = onItemSelected,
                                        sectionKey = SECTION_HERO,
                                        rowFocusRequester = heroFocusRequester,
                                        rowIsFocused = columnHasFocus && lastFocusedRowKey == ROW_HERO,
                                        upFocusRequester = previousRowFocusRequester(heroLazyIdx),
                                        downFocusRequester = nextRowFocusRequester(heroLazyIdx),
                                        onCarouselFocused = {
                                            lastFocusedRowKey = ROW_HERO
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

                    is HomeDashboardRow.Section ->
                        homeSectionItem(row.index, feed.sections[row.index])
                }
            }
        }
    }
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
private const val ROW_CONTINUE_WATCHING = "continue_watching"
private const val ROW_HERO = "hero_carousel"
private val ROW_FOCUS_TIMEOUT = 500.milliseconds

/** Ряд главной на ТВ: блоки без своего ряда (видео блогеров, быстрые действия) сюда не попадают. */
private sealed interface HomeDashboardRow {
    data object ContinueWatching : HomeDashboardRow

    data object Hero : HomeDashboardRow

    data class Section(val index: Int) : HomeDashboardRow
}
