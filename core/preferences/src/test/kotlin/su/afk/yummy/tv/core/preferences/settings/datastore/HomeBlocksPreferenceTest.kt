package su.afk.yummy.tv.core.preferences.settings.datastore

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.HomeBlockConfig
import su.afk.yummy.tv.core.model.settings.HomeBlockKind

class HomeBlocksPreferenceTest {

    @Test
    fun `missing value gives defaults and migrates the legacy new episodes toggle`() {
        val blocks = null.toHomeBlocks(legacyNewEpisodesEnabled = false)

        assertEquals(HomeBlockKind.entries, blocks.map { it.kind })
        assertEquals(
            listOf(HomeBlockKind.MY_NEW_EPISODES),
            blocks.filterNot { it.enabled }.map { it.kind },
        )
    }

    @Test
    fun `round trip keeps order and visibility`() {
        val source = listOf(
            HomeBlockConfig(HomeBlockKind.COLLECTIONS, enabled = false),
            HomeBlockConfig(HomeBlockKind.HERO),
        )

        val restored = source.toHomeBlocksPreference().toHomeBlocks()

        assertEquals(source, restored.take(2))
        assertEquals(HomeBlockKind.entries.size, restored.size)
    }

    @Test
    fun `unknown and duplicate entries are dropped and missing ones appended`() {
        val restored = "HERO|GONE|HERO:0|QUICK_ACTIONS".toHomeBlocks()

        assertEquals(HomeBlockKind.entries.size, restored.size)
        assertEquals(listOf(HomeBlockKind.HERO, HomeBlockKind.QUICK_ACTIONS), restored.take(2).map { it.kind })
        assertEquals(true, restored.first().enabled)
    }
}
