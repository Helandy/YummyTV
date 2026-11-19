package su.afk.yummy.tv.core.designsystem.mobile.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.floor

internal const val TAB_TRANSITION_EPSILON = 0.001f
private const val TAB_TRANSITION_DURATION_MS = 220
private const val PAGER_MAX_ANIMATE_PAGES = 3

/**
 * Состояние перехода между вкладками с непрерывным прогрессом [fraction] (`0f..1f`).
 *
 * В покое `fromIndex == toIndex` и `fraction == 0f`. Во время свайпа или анимации
 * перехода по тапу [fromIndex] — исходная вкладка, [toIndex] — целевая, а [fraction]
 * задаёт долю пути для плавного перетекания индикатора и цветов.
 */
@Immutable
data class MobileTabTransition(
    val fromIndex: Int,
    val toIndex: Int,
    val fraction: Float,
) {
    /** Переход завершён (или ещё не сдвинулся с исходной вкладки). */
    val isIdle: Boolean
        get() = fromIndex == toIndex ||
            fraction <= TAB_TRANSITION_EPSILON ||
            fraction >= 1f - TAB_TRANSITION_EPSILON

    /** Индекс вкладки, на которой сейчас сосредоточена большая часть индикатора. */
    val activeIndex: Int
        get() = if (fraction >= 0.5f) toIndex else fromIndex

    /** Вес выбранности вкладки [index] в диапазоне `0f..1f`. */
    fun selectionFraction(index: Int): Float = when {
        fromIndex == toIndex -> if (index == fromIndex) 1f else 0f
        index == fromIndex -> (1f - fraction).coerceIn(0f, 1f)
        index == toIndex -> fraction.coerceIn(0f, 1f)
        else -> 0f
    }

    companion object {
        /** Покоящееся состояние на вкладке [selectedIndex]. */
        fun settled(selectedIndex: Int): MobileTabTransition {
            val safe = selectedIndex.coerceAtLeast(0)
            return MobileTabTransition(fromIndex = safe, toIndex = safe, fraction = 0f)
        }
    }
}

/**
 * Программный переход по тапу между несоседними страницами `PagerState`.
 *
 * Позволяет анимировать индикатор напрямую из [fromIndex] в [toIndex] (например, между
 * соседними по вертикали плитками сетки или с плавным затуханием между дальними вкладками),
 * не мигая промежуточными страницами.
 */
@Immutable
internal data class ProgrammaticTabJump(
    val fromIndex: Int,
    val toIndex: Int,
    val startPosition: Float,
) {
    fun fraction(currentPosition: Float): Float {
        if (fromIndex == toIndex) return 1f
        // PagerState.animateScrollToPage при прыжке дальше 3 страниц сначала перескакивает
        // на расстояние 3 страниц от цели и плавно докручивает оставшийся участок.
        val effectiveStart = if (toIndex > fromIndex) {
            maxOf(startPosition, (toIndex - PAGER_MAX_ANIMATE_PAGES).toFloat())
        } else {
            minOf(startPosition, (toIndex + PAGER_MAX_ANIMATE_PAGES).toFloat())
        }
        val span = toIndex.toFloat() - effectiveStart
        if (abs(span) <= TAB_TRANSITION_EPSILON) return 1f
        return ((currentPosition - effectiveStart) / span).coerceIn(0f, 1f)
    }

    fun activeIndex(currentPosition: Float): Int =
        if (fraction(currentPosition) >= 0.5f) toIndex else fromIndex
}

/** Непрерывная позиция страницы pager'а (`0f..(pageCount - 1)`), без скачка на середине свайпа. */
internal fun calculatePagerPosition(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    pageCount: Int,
): Float {
    if (pageCount <= 1) return 0f
    return (currentPage + currentPageOffsetFraction).coerceIn(0f, (pageCount - 1).toFloat())
}

/** Строит [MobileTabTransition] из непрерывной позиции [position] между соседними страницами. */
internal fun mobileTabTransitionFromPagePosition(
    position: Float,
    tabCount: Int,
): MobileTabTransition {
    if (tabCount <= 0) return MobileTabTransition.settled(0)
    val maxIndex = tabCount - 1
    val clamped = position.coerceIn(0f, maxIndex.toFloat())
    val lower = floor(clamped).toInt().coerceIn(0, maxIndex)
    val upper = (lower + 1).coerceAtMost(maxIndex)
    val fraction = (clamped - lower).coerceIn(0f, 1f)
    return when {
        lower == upper || fraction <= TAB_TRANSITION_EPSILON -> MobileTabTransition.settled(lower)
        fraction >= 1f - TAB_TRANSITION_EPSILON -> MobileTabTransition.settled(upper)
        else -> MobileTabTransition(fromIndex = lower, toIndex = upper, fraction = fraction)
    }
}

/** Вычисляет [MobileTabTransition] по состоянию pager'а с учётом активного прыжка [jump]. */
internal fun resolvePagerTabTransition(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    pageCount: Int,
    jump: ProgrammaticTabJump? = null,
): MobileTabTransition {
    if (pageCount <= 0) return MobileTabTransition.settled(0)
    val maxIndex = pageCount - 1
    val position = calculatePagerPosition(currentPage, currentPageOffsetFraction, pageCount)
    if (jump != null) {
        val safeFrom = jump.fromIndex.coerceIn(0, maxIndex)
        val safeTo = jump.toIndex.coerceIn(0, maxIndex)
        val fraction = jump.fraction(position)
        return when {
            safeFrom == safeTo || fraction >= 1f - TAB_TRANSITION_EPSILON ->
                MobileTabTransition.settled(safeTo)
            else -> MobileTabTransition(fromIndex = safeFrom, toIndex = safeTo, fraction = fraction)
        }
    }
    return mobileTabTransitionFromPagePosition(position, pageCount)
}

/** Читает [MobileTabTransition] напрямую из [PagerState] (для экранов со своим pager'ом). */
fun PagerState.asMobileTabTransition(): MobileTabTransition = resolvePagerTabTransition(
    currentPage = currentPage,
    currentPageOffsetFraction = currentPageOffsetFraction,
    pageCount = pageCount,
)

/**
 * Плавно анимирует [MobileTabTransition] при смене [selectedIndex] для вкладок без `HorizontalPager`.
 * При развороте обратно к предыдущей вкладке продолжает движение с текущей точки без рывка.
 */
@Composable
fun rememberAnimatedTabTransition(
    selectedIndex: Int,
    tabCount: Int,
): MobileTabTransition {
    val maxIndex = (tabCount - 1).coerceAtLeast(0)
    val safeTarget = if (tabCount > 0) selectedIndex.coerceIn(0, maxIndex) else 0
    var fromIndex by remember { mutableIntStateOf(safeTarget) }
    var toIndex by remember { mutableIntStateOf(safeTarget) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(safeTarget, tabCount) {
        if (tabCount <= 0) {
            fromIndex = 0
            toIndex = 0
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        val currentVisual = (if (progress.value >= 0.5f) toIndex else fromIndex).coerceIn(0, maxIndex)
        if (currentVisual == safeTarget && toIndex == safeTarget && progress.value >= 1f - TAB_TRANSITION_EPSILON) {
            return@LaunchedEffect
        }
        val currentProgress = progress.value.coerceIn(0f, 1f)
        val isDirectReversal = fromIndex == safeTarget &&
            toIndex != safeTarget &&
            currentProgress > TAB_TRANSITION_EPSILON &&
            currentProgress < 1f - TAB_TRANSITION_EPSILON
        val startFraction = if (isDirectReversal) 1f - currentProgress else 0f
        fromIndex = if (isDirectReversal) toIndex.coerceIn(0, maxIndex) else currentVisual
        toIndex = safeTarget
        progress.snapTo(startFraction)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = TAB_TRANSITION_DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
        )
        fromIndex = safeTarget
    }

    val safeFrom = fromIndex.coerceIn(0, maxIndex)
    val safeTo = toIndex.coerceIn(0, maxIndex)
    val fraction = progress.value.coerceIn(0f, 1f)
    return if (safeFrom == safeTo || fraction >= 1f - TAB_TRANSITION_EPSILON) {
        MobileTabTransition.settled(safeTo)
    } else {
        MobileTabTransition(fromIndex = safeFrom, toIndex = safeTo, fraction = fraction)
    }
}
