package su.afk.yummy.tv.core.designsystem.mobile.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.floor
import kotlin.math.roundToInt

internal const val TAB_TRANSITION_EPSILON = 0.001f
internal const val TAB_TRANSITION_DURATION_MS = 220

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
 * Ведёт собственный [Animatable] от [startFraction] до `1f`, не полагаясь на внутреннюю
 * логику перескока `PagerState.animateScrollToPage`, поэтому промежуточные вкладки не мигают,
 * а разворот на полпути стартует ровно с текущего прогресса без рывка.
 */
@Stable
internal class ProgrammaticTabJump(
    val fromIndex: Int,
    val toIndex: Int,
    startFraction: Float = 0f,
) {
    private val animatable = Animatable(startFraction.coerceIn(0f, 1f))

    val fraction: Float
        get() = animatable.value.coerceIn(0f, 1f)

    val activeIndex: Int
        get() = if (fraction >= 0.5f) toIndex else fromIndex

    suspend fun animateToCompletion() {
        if (fromIndex == toIndex) {
            animatable.snapTo(1f)
            return
        }
        val remaining = (1f - fraction).coerceIn(0f, 1f)
        if (remaining <= TAB_TRANSITION_EPSILON) {
            animatable.snapTo(1f)
            return
        }
        val durationMs = (remaining * TAB_TRANSITION_DURATION_MS)
            .roundToInt()
            .coerceAtLeast(1)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMs,
                easing = FastOutSlowInEasing,
            ),
        )
    }
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
    if (jump != null) {
        val maxIndex = pageCount - 1
        val safeFrom = jump.fromIndex.coerceIn(0, maxIndex)
        val safeTo = jump.toIndex.coerceIn(0, maxIndex)
        val fraction = jump.fraction
        return when {
            safeFrom == safeTo || fraction >= 1f - TAB_TRANSITION_EPSILON ->
                MobileTabTransition.settled(safeTo)
            else -> MobileTabTransition(fromIndex = safeFrom, toIndex = safeTo, fraction = fraction)
        }
    }
    val position = calculatePagerPosition(currentPage, currentPageOffsetFraction, pageCount)
    return mobileTabTransitionFromPagePosition(position, pageCount)
}

/**
 * Читает [MobileTabTransition] напрямую из [PagerState] (для экранов со своим pager'ом).
 * Вызывайте внутри фазы отрисовки или в провайдере `() -> MobileTabTransition`, чтобы не
 * вызывать рекомпозицию на каждом кадре свайпа.
 */
fun PagerState.asMobileTabTransition(): MobileTabTransition = resolvePagerTabTransition(
    currentPage = currentPage,
    currentPageOffsetFraction = currentPageOffsetFraction,
    pageCount = pageCount,
)

/**
 * Возвращает провайдер `() -> MobileTabTransition`, плавно анимируемый при смене [selectedIndex]
 * для вкладок без `HorizontalPager`. Значение [Animatable] читается только внутри возвращаемой
 * лямбды (в фазе отрисовки), без покадровой рекомпозиции. При развороте обратно к предыдущей
 * вкладке движение продолжается с текущей точки без рывка.
 */
@Composable
internal fun rememberAnimatedTabTransition(
    selectedIndex: Int,
    tabCount: Int,
): () -> MobileTabTransition {
    val safeCount = tabCount.coerceAtLeast(0)
    val maxIndex = (safeCount - 1).coerceAtLeast(0)
    val safeTarget = if (safeCount > 0) selectedIndex.coerceIn(0, maxIndex) else 0
    var fromIndex by remember { mutableIntStateOf(safeTarget) }
    var toIndex by remember { mutableIntStateOf(safeTarget) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(safeTarget, safeCount) {
        if (safeCount <= 0) {
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
        val durationMs = ((1f - startFraction) * TAB_TRANSITION_DURATION_MS)
            .roundToInt()
            .coerceAtLeast(1)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMs,
                easing = FastOutSlowInEasing,
            ),
        )
        fromIndex = safeTarget
    }

    return remember(maxIndex) {
        {
            val safeFrom = fromIndex.coerceIn(0, maxIndex)
            val safeTo = toIndex.coerceIn(0, maxIndex)
            val fraction = progress.value.coerceIn(0f, 1f)
            if (safeFrom == safeTo || fraction >= 1f - TAB_TRANSITION_EPSILON) {
                MobileTabTransition.settled(safeTo)
            } else {
                MobileTabTransition(fromIndex = safeFrom, toIndex = safeTo, fraction = fraction)
            }
        }
    }
}
