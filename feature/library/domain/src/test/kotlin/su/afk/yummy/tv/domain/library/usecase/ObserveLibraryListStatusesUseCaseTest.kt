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
import su.afk.yummy.tv.domain.library.repository.LibraryRepository

class ObserveLibraryListStatusesUseCaseTest : BaseUnitTest() {

    private val repository: LibraryRepository = mockk()

    @Test
    fun `maps list ids to card statuses and skips favorite-only entries`() = runTest {
        every { repository.observeAll() } returns flowOf(
            listOf(
                localItem(1, UserAnimeList.WATCHING),
                localItem(2, UserAnimeList.POSTPONED),
                localItem(3, UserAnimeList.DROPPED, favorite = true),
                localItem(4, list = null, favorite = true),
            ),
        )

        val statuses = ObserveLibraryListStatusesUseCase(repository)().first()

        assertEquals(
            mapOf(
                1 to CardListStatus.WATCHING,
                2 to CardListStatus.POSTPONED,
                3 to CardListStatus.DROPPED,
            ),
            statuses,
        )
    }
}
