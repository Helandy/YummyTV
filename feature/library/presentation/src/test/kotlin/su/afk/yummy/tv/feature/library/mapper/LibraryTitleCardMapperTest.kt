package su.afk.yummy.tv.feature.library.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.CardListStatus
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.feature.library.model.LibraryTab

class LibraryTitleCardMapperTest {

    @Test
    fun `favorites tab shows the user list and hides the heart`() {
        val card = item().toTitleListCardUi(LibraryTab.FAVORITES, posterUrls = emptyList())

        assertEquals(CardListStatus.PLANNED, card.listStatus)
        assertFalse(card.isFavorite)
    }

    @Test
    fun `list tab shows the heart and hides the redundant list`() {
        val card = item().toTitleListCardUi(LibraryTab.PLANNED, posterUrls = listOf("poster"))

        assertNull(card.listStatus)
        assertTrue(card.isFavorite)
        assertEquals(listOf("poster"), card.posterUrls)
    }

    @Test
    fun `invalid ratings are dropped`() {
        val card = item(userRating = 0, rating = 0.0).toTitleListCardUi(LibraryTab.PLANNED, posterUrls = emptyList())

        assertNull(card.userRating)
        assertNull(card.siteRating)
    }

    private fun item(userRating: Int? = 9, rating: Double? = 8.5) = LibraryItem(
        animeId = 1,
        title = "Тайтл",
        listId = UserAnimeList.PLANNED.id,
        isFavorite = true,
        userRating = userRating,
        rating = rating,
    )
}
