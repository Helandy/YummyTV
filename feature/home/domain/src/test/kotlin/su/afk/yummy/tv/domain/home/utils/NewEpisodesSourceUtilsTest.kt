package su.afk.yummy.tv.domain.home.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.NewEpisodesSource
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.FAVORITE_ONLY_LIBRARY_LIST_ID
import su.afk.yummy.tv.domain.library.model.LibraryItem

class NewEpisodesSourceUtilsTest : BaseUnitTest() {

    @Test
    fun `every list source matches its account list id`() {
        val byList = mapOf(
            NewEpisodesSource.WATCHING to UserAnimeList.WATCHING,
            NewEpisodesSource.PLANNED to UserAnimeList.PLANNED,
            NewEpisodesSource.COMPLETED to UserAnimeList.COMPLETED,
            NewEpisodesSource.POSTPONED to UserAnimeList.POSTPONED,
            NewEpisodesSource.DROPPED to UserAnimeList.DROPPED,
        )

        byList.forEach { (source, list) ->
            assertTrue("$source", libraryItem(listId = list.id).matches(source))
        }
    }

    @Test
    fun `a list source does not match a different list`() {
        val item = libraryItem(listId = UserAnimeList.COMPLETED.id)

        assertFalse(item.matches(NewEpisodesSource.WATCHING))
        assertFalse(item.matches(NewEpisodesSource.DROPPED))
    }

    @Test
    fun `favorites match by flag regardless of the list`() {
        val item = libraryItem(listId = UserAnimeList.DROPPED.id, isFavorite = true)

        assertTrue(item.matches(NewEpisodesSource.FAVORITES))
    }

    @Test
    fun `a favorite-only row is not treated as watching`() {
        // Такие записи живут с listId = -1, и спутать их со «Смотрю» (id 0) нельзя.
        val item = libraryItem(listId = FAVORITE_ONLY_LIBRARY_LIST_ID, isFavorite = true)

        assertFalse(item.matches(NewEpisodesSource.WATCHING))
        assertTrue(item.matches(NewEpisodesSource.FAVORITES))
    }

    @Test
    fun `a non-favorite row never matches favorites`() {
        assertFalse(libraryItem(listId = UserAnimeList.WATCHING.id).matches(NewEpisodesSource.FAVORITES))
    }

    private fun libraryItem(listId: Int, isFavorite: Boolean = false) = LibraryItem(
        animeId = ANIME_ID,
        title = "Title",
        listId = listId,
        isFavorite = isFavorite,
    )

    private companion object {
        const val ANIME_ID = 42
    }
}
