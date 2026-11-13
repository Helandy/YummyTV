package su.afk.yummy.tv.feature.settings.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.DetailsSectionConfig
import su.afk.yummy.tv.core.model.settings.DetailsSectionKind
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection

class DetailsSectionUtilsTest {

    private val sections = DetailsSectionConfig.default

    @Test
    fun `move down swaps with the next section`() {
        val moved = sections.movedDetailsSection(DetailsSectionKind.INFO, DetailsButtonMoveDirection.DOWN)

        assertEquals(DetailsSectionKind.DESCRIPTION, moved[0].kind)
        assertEquals(DetailsSectionKind.INFO, moved[1].kind)
    }

    @Test
    fun `move past the edge keeps the order`() {
        assertEquals(sections, sections.movedDetailsSection(DetailsSectionKind.INFO, DetailsButtonMoveDirection.UP))
        assertEquals(sections, sections.movedDetailsSection(DetailsSectionKind.COMMENTS, DetailsButtonMoveDirection.DOWN))
    }

    @Test
    fun `toggle flips only the chosen section`() {
        val toggled = sections.toggledDetailsSection(DetailsSectionKind.REVIEWS)

        assertEquals(listOf(DetailsSectionKind.REVIEWS), toggled.filterNot { it.enabled }.map { it.kind })
        assertEquals(sections.map { it.kind }, toggled.map { it.kind })
    }
}
