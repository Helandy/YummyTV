package su.afk.yummy.tv.feature.main.mobile.view

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingNavigationWidthsTest {
    @Test
    fun `long labels fit by moving neighbouring icons`() {
        val widths = floatingNavigationWidths(328.dp, 5, 90.dp)

        assertEquals(138f, widths.selected.value, 0.01f)
        assertTrue(widths.unselected >= 44.dp)
        assertEquals(328f, (widths.selected + widths.unselected * 4).value, 0.01f)
    }

    @Test
    fun `compact width keeps space for every tab`() {
        val widths = floatingNavigationWidths(288.dp, 5, 100.dp)

        assertTrue(widths.unselected >= 44.dp)
        assertTrue(widths.selected >= 48.dp)
        assertEquals(288f, (widths.selected + widths.unselected * 4).value, 0.01f)
    }

    @Test
    fun `empty and unusually narrow layouts remain valid`() {
        assertEquals(FloatingNavigationWidths(0.dp, 0.dp), floatingNavigationWidths(300.dp, 0, 50.dp))
        val widths = floatingNavigationWidths(180.dp, 5, 120.dp)
        assertTrue(widths.selected >= 0.dp)
        assertTrue(widths.unselected >= 0.dp)
        assertEquals(180f, (widths.selected + widths.unselected * 4).value, 0.01f)
    }
}
