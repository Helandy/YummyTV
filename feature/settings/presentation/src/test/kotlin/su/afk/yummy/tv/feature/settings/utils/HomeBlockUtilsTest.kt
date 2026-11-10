package su.afk.yummy.tv.feature.settings.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.HomeBlockConfig
import su.afk.yummy.tv.core.model.settings.HomeBlockKind
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection

class HomeBlockUtilsTest {

    private val blocks = HomeBlockConfig.default

    @Test
    fun `move down swaps with the next block`() {
        val moved = blocks.movedHomeBlock(HomeBlockKind.HERO, DetailsButtonMoveDirection.DOWN)

        assertEquals(HomeBlockKind.CONTINUE_WATCHING, moved[0].kind)
        assertEquals(HomeBlockKind.HERO, moved[1].kind)
    }

    @Test
    fun `move at the edge keeps the order`() {
        assertEquals(blocks, blocks.movedHomeBlock(HomeBlockKind.HERO, DetailsButtonMoveDirection.UP))
        assertEquals(
            blocks,
            blocks.movedHomeBlock(HomeBlockKind.QUICK_ACTIONS, DetailsButtonMoveDirection.DOWN),
        )
    }

    @Test
    fun `toggle flips only the chosen block`() {
        val toggled = blocks.toggledHomeBlock(HomeBlockKind.COLLECTIONS)

        assertEquals(listOf(HomeBlockKind.COLLECTIONS), toggled.filterNot { it.enabled }.map { it.kind })
    }
}
