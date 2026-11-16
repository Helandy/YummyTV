package su.afk.yummy.tv.core.designsystem.mobile

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Держит [PagerState] и синхронизирует его с внешним выбором вкладки.
 *
 * Позволяет переключать вкладки и тапом (через [selectPage]), и горизонтальным свайпом
 * по [MobileSwipeableTabsPager]. Двусторонняя синхронизация настраивается в
 * [rememberMobileSwipeableTabsState].
 */
@Stable
class MobileSwipeableTabsState internal constructor(
    val pagerState: PagerState,
    private val scope: CoroutineScope,
) {
    /** Текущая (осевшая/докрученная) страница pager'а. */
    val currentPage: Int get() = pagerState.currentPage

    /** Анимированно докрутить pager к вкладке [index] (для onClick по табам). */
    fun selectPage(index: Int) {
        if (pagerState.currentPage != index) {
            scope.launch { pagerState.animateScrollToPage(index) }
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
        initialPage = selectedPage,
        pageCount = { pageCount },
    )
    val scope = rememberCoroutineScope()
    val state = remember(pagerState, scope) { MobileSwipeableTabsState(pagerState, scope) }

    // Внешний выбор вкладки -> двигаем pager.
    LaunchedEffect(selectedPage) {
        if (pagerState.currentPage != selectedPage) {
            pagerState.animateScrollToPage(selectedPage)
        }
    }

    // Свайп/докрутка pager'а -> сообщаем наружу, но только когда страница осела: промежуточные
    // страницы при докрутке через несколько вкладок иначе возвращали бы pager назад.
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != selectedPage) {
            onPageSelected(pagerState.settledPage)
        }
    }

    return state
}
