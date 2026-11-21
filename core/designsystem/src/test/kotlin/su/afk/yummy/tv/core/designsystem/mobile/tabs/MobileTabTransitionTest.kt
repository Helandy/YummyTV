package su.afk.yummy.tv.core.designsystem.mobile.tabs

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileTabTransitionTest {

    @Test
    fun `pager position stays continuous across half page boundary`() {
        val beforeMidpoint = calculatePagerPosition(currentPage = 0, currentPageOffsetFraction = 0.49f, pageCount = 2)
        val afterMidpoint = calculatePagerPosition(currentPage = 1, currentPageOffsetFraction = -0.49f, pageCount = 2)

        assertEquals(0.49f, beforeMidpoint, DELTA)
        assertEquals(0.51f, afterMidpoint, DELTA)

        val transitionBefore = resolvePagerTabTransition(0, 0.49f, pageCount = 2)
        val transitionAfter = resolvePagerTabTransition(1, -0.49f, pageCount = 2)

        assertEquals(0, transitionBefore.fromIndex)
        assertEquals(1, transitionBefore.toIndex)
        assertEquals(0.51f, transitionBefore.selectionFraction(0), DELTA)
        assertEquals(0.49f, transitionBefore.selectionFraction(1), DELTA)

        assertEquals(0, transitionAfter.fromIndex)
        assertEquals(1, transitionAfter.toIndex)
        assertEquals(0.49f, transitionAfter.selectionFraction(0), DELTA)
        assertEquals(0.51f, transitionAfter.selectionFraction(1), DELTA)
    }

    @Test
    fun `row adjacency matches only immediate horizontal neighbors`() {
        assertTrue(areAdjacentRowTabs(0, 1))
        assertTrue(areAdjacentRowTabs(1, 0))
        assertFalse(areAdjacentRowTabs(0, 0))
        assertFalse(areAdjacentRowTabs(0, 2))
        assertFalse(areAdjacentRowTabs(-1, 0))
    }

    @Test
    fun `grid adjacency matches horizontal and vertical neighbors but rejects row wrap`() {
        // Сетка 4×2: [0, 1, 2, 3] / [4, 5, 6, 7]
        assertTrue(areAdjacentGridTabs(fromIndex = 0, toIndex = 1, columns = 4))
        assertTrue(areAdjacentGridTabs(fromIndex = 5, toIndex = 4, columns = 4))
        assertTrue(areAdjacentGridTabs(fromIndex = 1, toIndex = 5, columns = 4))
        assertTrue(areAdjacentGridTabs(fromIndex = 6, toIndex = 2, columns = 4))

        // Перенос строки 3 -> 4, диагональ 0 -> 5 и прыжок через плитку 0 -> 2 не соседние.
        assertFalse(areAdjacentGridTabs(fromIndex = 3, toIndex = 4, columns = 4))
        assertFalse(areAdjacentGridTabs(fromIndex = 0, toIndex = 5, columns = 4))
        assertFalse(areAdjacentGridTabs(fromIndex = 0, toIndex = 2, columns = 4))
        assertFalse(areAdjacentGridTabs(fromIndex = 2, toIndex = 2, columns = 4))
    }

    @Test
    fun `fluid span stretches across adjacent gap and stays symmetric in both directions`() {
        val (midLeft, midRight) = interpolateFluidSpan(
            fromStart = 0f,
            fromEnd = 100f,
            toStart = 104f,
            toEnd = 204f,
            fraction = 0.5f,
        )
        assertTrue(midRight - midLeft > 100f)
        assertEquals(102f, (midLeft + midRight) / 2f, DELTA)

        val forward = interpolateFluidSpan(0f, 100f, 104f, 204f, fraction = 0.3f)
        val backward = interpolateFluidSpan(104f, 204f, 0f, 100f, fraction = 0.7f)
        assertEquals(forward.first, backward.first, DELTA)
        assertEquals(forward.second, backward.second, DELTA)
    }

    @Test
    fun `fluid rect stretches vertically for stacked grid tiles without shifting horizontal bounds`() {
        val topTile = Rect(left = 100f, top = 0f, right = 190f, bottom = 52f)
        val bottomTile = Rect(left = 100f, top = 54f, right = 190f, bottom = 106f)

        val mid = interpolateFluidRect(topTile, bottomTile, fraction = 0.5f)
        assertEquals(100f, mid.left, DELTA)
        assertEquals(190f, mid.right, DELTA)
        assertTrue(mid.height > topTile.height)
        assertEquals(53f, mid.center.y, DELTA)
    }

    @Test
    fun `renderTabIndicator glides toward offscreen neighbor in LazyRow for LTR and RTL`() {
        val anchor = HorizontalSpan(start = 220f, end = 300f)
        var blendedTo: HorizontalSpan? = null

        renderTabIndicator(
            transition = MobileTabTransition(fromIndex = 2, toIndex = 3, fraction = 0.4f),
            itemAt = { index -> if (index == 2) anchor else null },
            areAdjacent = ::areAdjacentRowTabs,
            fallbackAdjacent = { item, anchorIdx, missingIdx ->
                estimateAdjacentOffscreenSpan(item, anchorIdx, missingIdx, spacingPx = 10f, LayoutDirection.Ltr)
            },
            drawSingle = { _, _, _ -> error("expected fluid blend, not crossfade") },
            drawBlended = { _, to, _, _, _ -> blendedTo = to },
        )
        assertEquals(HorizontalSpan(start = 310f, end = 390f), blendedTo)

        val rtlEstimated = estimateAdjacentOffscreenSpan(
            anchor = HorizontalSpan(start = 20f, end = 100f),
            anchorIndex = 2,
            missingIndex = 3,
            spacingPx = 10f,
            layoutDirection = LayoutDirection.Rtl,
        )
        assertEquals(HorizontalSpan(start = -70f, end = 10f), rtlEstimated)
    }

    @Test
    fun `renderTabIndicator crossfades distant tabs smoothly with partial viewport visibility`() {
        // Прыжок 0 -> 7: вкладка 0 уходит влево (видна наполовину), вкладка 7 ещё за правым краем.
        val halfVisibleFrom = HorizontalSpan(start = -40f, end = 40f)
        val drawnPhase1 = mutableListOf<Pair<Int, Float>>()
        renderTabIndicator(
            transition = MobileTabTransition(fromIndex = 0, toIndex = 7, fraction = 0.25f),
            itemAt = { index -> if (index == 0) halfVisibleFrom else null },
            areAdjacent = ::areAdjacentRowTabs,
            crossfadeVisibility = { it.visibilityFraction(viewportStart = 0f, viewportEnd = 300f) },
            drawSingle = { _, index, alpha -> drawnPhase1 += index to alpha },
            drawBlended = { _, _, _, _, _ -> error("distant tabs must not stretch across middle items") },
        )
        assertEquals(1, drawnPhase1.size)
        assertEquals(0, drawnPhase1.single().first)
        assertEquals(0.75f * 0.5f, drawnPhase1.single().second, DELTA)

        // Вторая фаза: вкладка 0 скрылась, вкладка 7 на четверть показалась справа.
        val quarterVisibleTo = HorizontalSpan(start = 280f, end = 360f)
        val drawnPhase2 = mutableListOf<Pair<Int, Float>>()
        renderTabIndicator(
            transition = MobileTabTransition(fromIndex = 0, toIndex = 7, fraction = 0.8f),
            itemAt = { index -> if (index == 7) quarterVisibleTo else null },
            areAdjacent = ::areAdjacentRowTabs,
            crossfadeVisibility = { it.visibilityFraction(viewportStart = 0f, viewportEnd = 300f) },
            drawSingle = { _, index, alpha -> drawnPhase2 += index to alpha },
            drawBlended = { _, _, _, _, _ -> error("distant tabs must not stretch across middle items") },
        )
        assertEquals(1, drawnPhase2.size)
        assertEquals(7, drawnPhase2.single().first)
        assertEquals(0.8f * 0.25f, drawnPhase2.single().second, DELTA)
    }

    @Test
    fun `lazyRowScrollTarget heads to destination immediately for distant jumps only`() {
        assertEquals(7, MobileTabTransition(fromIndex = 0, toIndex = 7, fraction = 0.1f).lazyRowScrollTarget())
        assertEquals(0, MobileTabTransition(fromIndex = 0, toIndex = 1, fraction = 0.3f).lazyRowScrollTarget())
        assertEquals(1, MobileTabTransition(fromIndex = 0, toIndex = 1, fraction = 0.6f).lazyRowScrollTarget())
    }

    private companion object {
        const val DELTA = 0.001f
    }
}
