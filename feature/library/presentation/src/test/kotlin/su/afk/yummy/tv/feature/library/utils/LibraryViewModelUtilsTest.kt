package su.afk.yummy.tv.feature.library.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.LibrarySort
import su.afk.yummy.tv.core.model.settings.LibrarySortDirection
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.feature.library.LibraryState
import su.afk.yummy.tv.feature.library.model.LibraryTab

class LibraryViewModelUtilsTest : BaseUnitTest() {

    /** Two rows for one title mean a duplicate key in the grid, which crashes Compose. */
    @Test
    fun `a tab never holds two cards of the same title`() {
        val duplicated = LibraryItem(
            animeId = 855389,
            title = "Title",
            listId = LibraryTab.WATCHING.userAnimeList()!!.id,
        )

        val tabs = buildLibraryTabItems(
            items = listOf(duplicated, duplicated.copy(title = "Title (dub)")),
            sort = LibrarySort.TITLE,
            direction = LibrarySortDirection.ASC,
        )

        assertEquals(1, tabs[LibraryTab.WATCHING]?.size)
    }

    @Test
    fun `tab is loading until its local source emits`() {
        val initial = LibraryState.State()
        val libraryOnly = initial.copy(isLibraryLoaded = true)

        assertTrue(initial.isLocalLoading(LibraryTab.CONTINUE_WATCHING))
        assertTrue(initial.isLocalLoading(LibraryTab.WATCHING))
        assertFalse(libraryOnly.isLocalLoading(LibraryTab.FAVORITES))
        assertTrue(libraryOnly.isLocalLoading(LibraryTab.CONTINUE_WATCHING))
        assertFalse(initial.copy(isContinueWatchingLoaded = true).isLocalLoading(LibraryTab.CONTINUE_WATCHING))
    }

    /** History has its own Paging load state. */
    @Test
    fun `history never waits for local lists`() {
        assertFalse(LibraryState.State().isLocalLoading(LibraryTab.HISTORY))
    }
}
