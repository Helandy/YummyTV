package su.afk.yummy.tv.domain.library.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeReleaseStatus
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.library.model.LibraryItem

class LibrarySyncUtilsTest {

    @Test
    fun `remote kind and release status replace local ones`() {
        val result = remote(kind = "OVA", releaseStatus = AnimeReleaseStatus.RELEASED)
            .toLibraryItem(current = local(), now = 0L, listId = 0, isFavorite = false, listUpdatedAt = 0L, favoriteUpdatedAt = 0L)

        assertEquals("OVA", result.kind)
        assertEquals(AnimeReleaseStatus.RELEASED, result.releaseStatus)
    }

    @Test
    fun `missing remote kind and release status keep local ones`() {
        val result = remote(kind = null, releaseStatus = null)
            .toLibraryItem(current = local(), now = 0L, listId = 0, isFavorite = false, listUpdatedAt = 0L, favoriteUpdatedAt = 0L)

        assertEquals("ТВ", result.kind)
        assertEquals(AnimeReleaseStatus.ONGOING, result.releaseStatus)
    }

    private fun local() = LibraryItem(
        animeId = 1,
        title = "Тайтл",
        kind = "ТВ",
        releaseStatus = AnimeReleaseStatus.ONGOING,
    )

    private fun remote(kind: String?, releaseStatus: AnimeReleaseStatus?) = UserAnimeListItem(
        animeId = 1,
        title = "Тайтл",
        posterUrl = null,
        rating = null,
        year = null,
        list = null,
        isFavorite = false,
        kind = kind,
        releaseStatus = releaseStatus,
    )
}
