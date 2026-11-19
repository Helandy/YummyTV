package su.afk.yummy.tv.domain.library.usecase

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.CardListStatus
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.repository.LibraryRepository

class ObserveLibraryAnimeIdsUseCaseTest : BaseUnitTest() {

    private val repository: LibraryRepository = mockk()

    @Test
    fun `by default includes watching, planned, and favorites while excluding dropped, postponed, and completed`() =
        runTest {
            every { repository.observeAll() } returns flowOf(
                listOf(
                    localItem(1, UserAnimeList.WATCHING),
                    localItem(2, UserAnimeList.PLANNED),
                    localItem(3, list = null, favorite = true),
                    localItem(4, UserAnimeList.DROPPED),
                    localItem(5, UserAnimeList.POSTPONED),
                    localItem(6, UserAnimeList.COMPLETED),
                    localItem(7, UserAnimeList.COMPLETED, favorite = true),
                    LibraryItem(animeId = 8, title = "orphan", listId = -99, isFavorite = false),
                ),
            )

            val ids = ObserveLibraryAnimeIdsUseCase(repository)().first()

            assertEquals(setOf(1, 2, 3, 7), ids)
        }

    @Test
    fun `supports custom status filter and disabling favorites`() = runTest {
        every { repository.observeAll() } returns flowOf(
            listOf(
                localItem(1, UserAnimeList.WATCHING),
                localItem(2, UserAnimeList.DROPPED),
                localItem(3, list = null, favorite = true),
            ),
        )

        val allStatusesWithoutFavs = ObserveLibraryAnimeIdsUseCase(repository)(
            statuses = CardListStatus.entries.toSet(),
            includeFavorites = false,
        ).first()

        assertEquals(setOf(1, 2), allStatusesWithoutFavs)
    }
}
