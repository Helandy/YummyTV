package su.afk.yummy.tv.feature.catalog.navigator

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.core.testing.BaseUnitTest

class CatalogSearchOpenerTest : BaseUnitTest() {

    private val nav: INavigationManager = mockk(relaxed = true)

    @Test
    fun `open search switches to the catalog tab and keeps the request until consumed`() {
        val opener = CatalogSearchOpener(nav)

        opener.openSearch()

        verify(exactly = 1) { nav.switchRoot(RootTab.CATALOG, true) }
        assertTrue(opener.isSearchFocusPending.value)

        opener.consumeSearchFocus()

        assertFalse(opener.isSearchFocusPending.value)
    }
}
