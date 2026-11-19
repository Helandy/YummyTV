package su.afk.yummy.tv.core.designsystem.mobile.tabs

import androidx.compose.ui.geometry.Rect
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
        // В середине перехода плашка шире исходной (100px) и симметрично перекрывает зазор.
        assertTrue(midRight - midLeft > 100f)
        assertEquals(102f, (midLeft + midRight) / 2f, DELTA)

        // Прямой переход 0 -> 1 в точке t совпадает с обратным 1 -> 0 в точке (1 - t).
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
    fun `programmatic jump across four pages smoothly maps post-snap span from zero to one`() {
        // Тап с плитки 1 на плитку 5 под ней в сетке 4×2: pager перескакивает на 2 и докручивает 2 -> 5.
        val jump = ProgrammaticTabJump(fromIndex = 1, toIndex = 5, startPosition = 1f)

        val beforeSnap = resolvePagerTabTransition(1, 0f, pageCount = 8, jump = jump)
        assertEquals(1, beforeSnap.fromIndex)
        assertEquals(5, beforeSnap.toIndex)
        assertEquals(0f, beforeSnap.fraction, DELTA)
        assertTrue(beforeSnap.isIdle)

        val atSnap = resolvePagerTabTransition(2, 0f, pageCount = 8, jump = jump)
        assertEquals(0f, atSnap.fraction, DELTA)

        val halfway = resolvePagerTabTransition(3, 0.5f, pageCount = 8, jump = jump)
        assertEquals(1, halfway.fromIndex)
        assertEquals(5, halfway.toIndex)
        assertEquals(0.5f, halfway.fraction, DELTA)
        assertFalse(halfway.isIdle)

        val finished = resolvePagerTabTransition(5, 0f, pageCount = 8, jump = jump)
        assertTrue(finished.isIdle)
        assertEquals(5, finished.activeIndex)
        assertEquals(1f, finished.selectionFraction(5), DELTA)
    }

    private companion object {
        const val DELTA = 0.001f
    }
}
