package su.afk.yummy.tv.domain.library.usecase

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.repository.LibraryRepository

class ObserveLibraryAnimeIdsUseCaseTest : BaseUnitTest() {

    private val repository: LibraryRepository = mockk()

    @Test
    fun `includes all user lists and favorite-only entries and ignores orphan items`() = runTest {
        every { repository.observeAll() } returns flowOf(
            listOf(
                localItem(1, UserAnimeList.WATCHING),
                localItem(2, UserAnimeList.PLANNED),
                localItem(3, list = null, favorite = true),
                LibraryItem(animeId = 4, title = "orphan", listId = -99, isFavorite = false),
            ),
        )

        val ids = ObserveLibraryAnimeIdsUseCase(repository)().first()

        assertEquals(setOf(1, 2, 3), ids)
    }
}
