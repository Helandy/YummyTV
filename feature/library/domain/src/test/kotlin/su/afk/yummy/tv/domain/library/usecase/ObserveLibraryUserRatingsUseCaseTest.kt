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
import su.afk.yummy.tv.domain.library.repository.LibraryRepository

class ObserveLibraryUserRatingsUseCaseTest : BaseUnitTest() {

    private val repository: LibraryRepository = mockk()

    @Test
    fun `maps rated entries and skips missing or out-of-range ratings`() = runTest {
        every { repository.observeAll() } returns flowOf(
            listOf(
                localItem(1, UserAnimeList.WATCHING).copy(userRating = 9),
                localItem(2, list = null, favorite = true).copy(userRating = 10),
                localItem(3, UserAnimeList.COMPLETED),
                localItem(4, UserAnimeList.DROPPED).copy(userRating = 0),
            ),
        )

        val ratings = ObserveLibraryUserRatingsUseCase(repository)().first()

        assertEquals(mapOf(1 to 9, 2 to 10), ratings)
    }
}
