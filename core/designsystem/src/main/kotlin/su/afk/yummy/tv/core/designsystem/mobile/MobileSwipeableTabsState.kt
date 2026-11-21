package su.afk.yummy.tv.core.designsystem.mobile

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTabTransition
import su.afk.yummy.tv.core.designsystem.mobile.tabs.ProgrammaticTabJump
import su.afk.yummy.tv.core.designsystem.mobile.tabs.TAB_TRANSITION_EPSILON
import su.afk.yummy.tv.core.designsystem.mobile.tabs.calculatePagerPosition
import su.afk.yummy.tv.core.designsystem.mobile.tabs.resolvePagerTabTransition
import kotlin.math.abs
import kotlin.math.roundToInt

/** Тонкая обёртка над [PagerState], позволяющая тестировать [MobileSwipeableTabsState] в JVM-тестах. */
internal interface TabPagerAdapter {
    val currentPage: Int
    val currentPageOffsetFraction: Float
    val targetPage: Int
    val pageCount: Int
    val interactions: Flow<Interaction>
    suspend fun animateScrollToPage(page: Int)
}

private class DefaultTabPagerAdapter(
    private val pagerState: PagerState,
) : TabPagerAdapter {
    override val currentPage: Int get() = pagerState.currentPage
    override val currentPageOffsetFraction: Float get() = pagerState.currentPageOffsetFraction
    override val targetPage: Int get() = pagerState.targetPage
    override val pageCount: Int get() = pagerState.pageCount
    override val interactions: Flow<Interaction> get() = pagerState.interactionSource.interactions
    override suspend fun animateScrollToPage(page: Int) = pagerState.animateScrollToPage(page)
}

/**
 * Держит [PagerState] и синхронизирует его с внешним выбором вкладки.
 *
 * Позволяет переключать вкладки и тапом (через [selectPage]), и горизонтальным свайпом
 * по [MobileSwipeableTabsPager], а также отдаёт [tabTransition] с непрерывным прогрессом
 * для плавной анимации индикатора вкладок. Передавайте `tabsState::tabTransition` в компоненты
 * вкладок, чтобы прогресс читался только в фазе отрисовки без покадровой рекомпозиции экрана.
 */
@Stable
class MobileSwipeableTabsState private constructor(
    private val pager: TabPagerAdapter,
    private val scope: CoroutineScope,
    private val ownedPagerState: PagerState?,
) {
    internal constructor(
        pagerState: PagerState,
        scope: CoroutineScope,
    ) : this(
        pager = DefaultTabPagerAdapter(pagerState),
        scope = scope,
        ownedPagerState = pagerState,
    )

    internal constructor(
        pager: TabPagerAdapter,
        scope: CoroutineScope,
    ) : this(
        pager = pager,
        scope = scope,
        ownedPagerState = null,
    )

    val pagerState: PagerState
        get() = checkNotNull(ownedPagerState) { "PagerState is only available in Compose runtime" }

    private var programmaticJump by mutableStateOf<ProgrammaticTabJump?>(null)
    private var scrollJob: Job? = null

    /** Текущая страница pager'а. */
    val currentPage: Int get() = pager.currentPage

    /** Непрерывное состояние перехода между вкладками (`0f..1f`), синхронизированное со свайпом и тапом. */
    val tabTransition: MobileTabTransition
        get() = resolvePagerTabTransition(
            currentPage = pager.currentPage,
            currentPageOffsetFraction = pager.currentPageOffsetFraction,
            pageCount = pager.pageCount,
            jump = programmaticJump,
        )

    /** Анимированно докрутить pager к вкладке [index] (для `onClick` по табам, включая разворот на полпути). */
    fun selectPage(index: Int) {
        val count = pager.pageCount
        if (index !in 0 until count) return
        val activeJump = programmaticJump
        if (activeJump != null && activeJump.toIndex == index && scrollJob?.isActive == true) {
            return
        }
        if (activeJump == null && isSettledAt(index)) {
            return
        }
        val nextJump = createJumpIfNeeded(target = index, count = count)
        val previousJob = scrollJob
        programmaticJump = nextJump
        previousJob?.cancel()
        val job = scope.launch {
            runScroll(target = index, jump = nextJump)
        }
        scrollJob = job
    }

    internal suspend fun animateToExternalPage(index: Int) {
        val count = pager.pageCount
        if (index !in 0 until count) return
        val activeJump = programmaticJump
        if (activeJump != null && activeJump.toIndex == index && scrollJob?.isActive == true) {
            return
        }
        if (activeJump == null && (isSettledAt(index) || pager.targetPage == index)) {
            return
        }
        val nextJump = createJumpIfNeeded(target = index, count = count)
        val previousJob = scrollJob
        programmaticJump = nextJump
        previousJob?.cancel()
        scrollJob = null
        runScroll(target = index, jump = nextJump)
    }

    /** При начале ручного перетаскивания отменяет программный прыжок и возвращает управление пальцу. */
    internal suspend fun observeUserDragCancellation() {
        pager.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                cancelProgrammaticJump()
            }
        }
    }

    internal fun cancelProgrammaticJump() {
        val hadJump = programmaticJump != null
        programmaticJump = null
        if (hadJump) {
            scrollJob?.cancel()
            scrollJob = null
        }
    }

    private suspend fun runScroll(
        target: Int,
        jump: ProgrammaticTabJump?,
    ) {
        if (jump == null) {
            pager.animateScrollToPage(target)
            return
        }
        try {
            coroutineScope {
                launch { jump.animateToCompletion() }
                pager.animateScrollToPage(target)
            }
        } finally {
            if (programmaticJump === jump) {
                programmaticJump = null
            }
        }
    }

    private fun isSettledAt(target: Int): Boolean =
        pager.currentPage == target && abs(pager.currentPageOffsetFraction) <= TAB_TRANSITION_EPSILON

    private fun createJumpIfNeeded(
        target: Int,
        count: Int,
    ): ProgrammaticTabJump? {
        val maxIndex = count - 1
        val prevJump = programmaticJump
        if (prevJump != null) {
            val prevFraction = prevJump.fraction
            val isMidFlight = prevFraction > TAB_TRANSITION_EPSILON &&
                prevFraction < 1f - TAB_TRANSITION_EPSILON
            if (prevJump.fromIndex == target && prevJump.toIndex != target && isMidFlight) {
                return ProgrammaticTabJump(
                    fromIndex = prevJump.toIndex.coerceIn(0, maxIndex),
                    toIndex = target,
                    startFraction = 1f - prevFraction,
                )
            }
            val activeFrom = prevJump.activeIndex.coerceIn(0, maxIndex)
            return ProgrammaticTabJump(
                fromIndex = activeFrom,
                toIndex = target,
                startFraction = if (activeFrom == target) 1f else 0f,
            )
        }

        val position = calculatePagerPosition(
            currentPage = pager.currentPage,
            currentPageOffsetFraction = pager.currentPageOffsetFraction,
            pageCount = count,
        )
        if (abs(target.toFloat() - position) <= 1f + TAB_TRANSITION_EPSILON) {
            return null
        }
        val activeFrom = position.roundToInt().coerceIn(0, maxIndex)
        return ProgrammaticTabJump(
            fromIndex = activeFrom,
            toIndex = target,
            startFraction = 0f,
        )
    }
}

/**
 * Создаёт [MobileSwipeableTabsState] для свайпаемых вкладок.
 *
 * @param selectedPage индекс выбранной вкладки во внешнем состоянии.
 * @param pageCount количество вкладок/страниц.
 * @param onPageSelected вызывается, когда свайп/докрутка сменили страницу — тут обычно
 *   диспатчится событие смены выбранной вкладки в состояние.
 */
@Composable
fun rememberMobileSwipeableTabsState(
    selectedPage: Int,
    pageCount: Int,
    onPageSelected: (Int) -> Unit,
): MobileSwipeableTabsState {
    val pagerState = rememberPagerState(
        initialPage = selectedPage.coerceAtLeast(0),
        pageCount = { pageCount },
    )
    val scope = rememberCoroutineScope()
    val state = remember(pagerState, scope) { MobileSwipeableTabsState(pagerState, scope) }
    val currentSelectedPage by rememberUpdatedState(selectedPage)
    val currentPageCount by rememberUpdatedState(pageCount)
    val currentOnPageSelected by rememberUpdatedState(onPageSelected)

    LaunchedEffect(state) {
        state.observeUserDragCancellation()
    }

    // Внешний выбор вкладки -> двигаем pager.
    LaunchedEffect(selectedPage) {
        state.animateToExternalPage(selectedPage)
    }

    // Свайп/докрутка pager'а -> сообщаем наружу, но только когда страница осела: промежуточные
    // страницы при докрутке через несколько вкладок иначе возвращали бы pager назад.
    LaunchedEffect(pagerState.settledPage) {
        if (currentPageCount > 0 && pagerState.settledPage != currentSelectedPage) {
            currentOnPageSelected(pagerState.settledPage)
        }
    }

    return state
}
