package su.afk.yummy.tv.feature.library.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.LibrarySort
import su.afk.yummy.tv.core.model.settings.LibrarySortDirection
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.feature.library.model.LibraryTab

class LibraryViewModelUtilsTest {

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
}
