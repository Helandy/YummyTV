package su.afk.yummy.tv.core.designsystem.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Состояние схлопывающейся шапки: [offsetPx] от `-headerHeight` (шапка уехала) до 0 (раскрыта).
 *
 * Листание вверх сначала уводит шапку и только потом скроллит список; листание вниз
 * сначала докручивает список до начала и только потом возвращает шапку.
 */
@Stable
class MobileCollapsingHeaderState internal constructor(initialOffset: Float = 0f) {
    internal var headerHeight by mutableIntStateOf(0)

    /** Сколько шапки остаётся сверху в схлопнутом виде (под статус-баром у edge-to-edge экранов). */
    internal var collapsedTopInset by mutableIntStateOf(0)

    private var rawOffset by mutableFloatStateOf(initialOffset)

    private val maxCollapse: Float
        get() = (headerHeight - collapsedTopInset).coerceAtLeast(0).toFloat()

    val offsetPx: Float
        get() = rawOffset.coerceIn(-maxCollapse, 0f)

    /** 0 — шапка раскрыта, 1 — полностью схлопнута. */
    val collapseFraction: Float
        get() = if (maxCollapse <= 0f) 0f else (-offsetPx / maxCollapse).coerceIn(0f, 1f)

    private fun consume(deltaY: Float): Float {
        val current = offsetPx
        val next = (current + deltaY).coerceIn(-maxCollapse, 0f)
        rawOffset = next
        return next - current
    }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (available.y < 0f) Offset(0f, consume(available.y)) else Offset.Zero

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (available.y > 0f) Offset(0f, consume(available.y)) else Offset.Zero
    }

    internal companion object {
        // Смещение переживает уход экрана из композиции (переход вглубь и назад): иначе шапка
        // раскрывается поверх списка, который сохранил свою прокрутку.
        val Saver: Saver<MobileCollapsingHeaderState, Float> = Saver(
            save = { it.rawOffset },
            restore = { MobileCollapsingHeaderState(initialOffset = it) },
        )
    }
}

@Composable
fun rememberMobileCollapsingHeaderState(): MobileCollapsingHeaderState =
    rememberSaveable(saver = MobileCollapsingHeaderState.Saver) { MobileCollapsingHeaderState() }

/**
 * Шапка [header] уезжает при листании [content], а [stickyHeader] (например, вкладки)
 * прилипает к верху. [content] всегда получает высоту «экран минус [stickyHeader]»,
 * поэтому со схлопнутой шапкой список занимает весь экран. [content] должен быть
 * скроллируемым (LazyColumn и т. п.) — он и двигает шапку через nested scroll.
 */
@Composable
fun MobileCollapsingHeaderLayout(
    header: @Composable () -> Unit,
    stickyHeader: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    state: MobileCollapsingHeaderState = rememberMobileCollapsingHeaderState(),
    /** Отступ сверху, на котором останавливается [stickyHeader], — например высота статус-бара. */
    collapsedTopInset: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Layout(
        contents = listOf(header, stickyHeader, content),
        modifier = modifier
            .clipToBounds()
            .nestedScroll(state.nestedScrollConnection),
    ) { (headerMeasurables, stickyMeasurables, contentMeasurables), constraints ->
        val loose = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        val headerPlaceables = headerMeasurables.map { it.measure(loose) }
        val stickyPlaceables = stickyMeasurables.map { it.measure(loose) }
        val headerHeight = headerPlaceables.sumOf { it.height }
        val stickyHeight = stickyPlaceables.sumOf { it.height }
        val topInset = collapsedTopInset.roundToPx()
        state.headerHeight = headerHeight
        state.collapsedTopInset = topInset

        val contentHeight = (constraints.maxHeight - stickyHeight - topInset).coerceAtLeast(0)
        val contentPlaceables = contentMeasurables.map {
            it.measure(constraints.copy(minHeight = contentHeight, maxHeight = contentHeight))
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            var y = state.offsetPx.roundToInt()
            headerPlaceables.forEach {
                it.placeRelative(0, y)
                y += it.height
            }
            stickyPlaceables.forEach {
                it.placeRelative(0, y)
                y += it.height
            }
            contentPlaceables.forEach { it.placeRelative(0, y) }
        }
    }
}
