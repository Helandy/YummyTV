package su.afk.yummy.tv.feature.catalog.navigator

import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.core.testing.BaseUnitTest

class CatalogSearchOpenerTest : BaseUnitTest() {

    private val nav: INavigationManager = mockk(relaxed = true)

    @Test
    fun `open search switches to the catalog tab and resets home and catalog stacks and keeps the request until consumed`() {
        val opener = CatalogSearchOpener(nav)

        opener.openSearch()

        verifyOrder {
            nav.resetRoot(RootTab.HOME)
            nav.resetRoot(RootTab.CATALOG)
            nav.switchRoot(RootTab.CATALOG, true)
        }
        verify(exactly = 0) { nav.resetRoot(RootTab.POSTS) }
        assertTrue(opener.isSearchFocusPending.value)

        opener.consumeSearchFocus()

        assertFalse(opener.isSearchFocusPending.value)
    }
}
