package su.afk.yummy.tv.core.designsystem.mobile

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
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTabTransition
import su.afk.yummy.tv.core.designsystem.mobile.tabs.ProgrammaticTabJump
import su.afk.yummy.tv.core.designsystem.mobile.tabs.TAB_TRANSITION_EPSILON
import su.afk.yummy.tv.core.designsystem.mobile.tabs.calculatePagerPosition
import su.afk.yummy.tv.core.designsystem.mobile.tabs.resolvePagerTabTransition
import kotlin.math.abs

/**
 * Держит [PagerState] и синхронизирует его с внешним выбором вкладки.
 *
 * Позволяет переключать вкладки и тапом (через [selectPage]), и горизонтальным свайпом
 * по [MobileSwipeableTabsPager], а также отдаёт [tabTransition] с непрерывным прогрессом
 * для плавной анимации индикатора вкладок. Двусторонняя синхронизация настраивается в
 * [rememberMobileSwipeableTabsState].
 */
@Stable
class MobileSwipeableTabsState internal constructor(
    val pagerState: PagerState,
    private val scope: CoroutineScope,
) {
    private var programmaticJump by mutableStateOf<ProgrammaticTabJump?>(null)

    /** Текущая страница pager'а. */
    val currentPage: Int get() = pagerState.currentPage

    /** Непрерывное состояние перехода между вкладками (`0f..1f`), синхронизированное со свайпом и тапом. */
    val tabTransition: MobileTabTransition
        get() = resolvePagerTabTransition(
            currentPage = pagerState.currentPage,
            currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
            pageCount = pagerState.pageCount,
            jump = programmaticJump,
        )

    /** Анимированно докрутить pager к вкладке [index] (для `onClick` по табам, включая разворот на полпути). */
    fun selectPage(index: Int) {
        val count = pagerState.pageCount
        if (index !in 0 until count) return
        if (isSettledAt(index)) {
            programmaticJump = null
            return
        }
        val jump = createJumpIfNeeded(target = index, count = count)
        programmaticJump = jump
        scope.launch {
            try {
                pagerState.animateScrollToPage(index)
            } finally {
                if (programmaticJump === jump) {
                    programmaticJump = null
                }
            }
        }
    }

    internal suspend fun animateToExternalPage(index: Int) {
        val count = pagerState.pageCount
        if (index !in 0 until count) return
        if (isSettledAt(index) || pagerState.targetPage == index) return
        val jump = createJumpIfNeeded(target = index, count = count)
        programmaticJump = jump
        try {
            pagerState.animateScrollToPage(index)
        } finally {
            if (programmaticJump === jump) {
                programmaticJump = null
            }
        }
    }

    private fun isSettledAt(target: Int): Boolean =
        pagerState.currentPage == target && abs(pagerState.currentPageOffsetFraction) <= TAB_TRANSITION_EPSILON

    private fun createJumpIfNeeded(target: Int, count: Int): ProgrammaticTabJump? {
        val position = calculatePagerPosition(
            currentPage = pagerState.currentPage,
            currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
            pageCount = count,
        )
        val activeFrom = programmaticJump?.activeIndex(position) ?: pagerState.currentPage.coerceIn(0, count - 1)
        return if (abs(target - activeFrom) > 1) {
            ProgrammaticTabJump(
                fromIndex = activeFrom,
                toIndex = target,
                startPosition = position,
            )
        } else {
            null
        }
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
