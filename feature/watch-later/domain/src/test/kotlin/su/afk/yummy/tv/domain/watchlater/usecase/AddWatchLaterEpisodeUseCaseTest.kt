package su.afk.yummy.tv.domain.watchlater.usecase

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.utils.coroutines.AppClock
import su.afk.yummy.tv.domain.watchlater.model.WatchLaterItem
import su.afk.yummy.tv.domain.watchlater.repository.WatchLaterRepository

class AddWatchLaterEpisodeUseCaseTest : BaseUnitTest() {

    private val repository: WatchLaterRepository = mockk(relaxed = true)
    private val clock: AppClock = mockk { every { nowMillis() } returns NOW }

    @Test
    fun `episode is postponed with the current time`() = runTest {
        AddWatchLaterEpisodeUseCase(repository, clock)(42, "3", "Title", "poster", "shot")

        coVerify { repository.add(WatchLaterItem(42, "3", "Title", "poster", "shot", addedAt = NOW)) }
    }

    @Test
    fun `unknown title or episode is ignored`() = runTest {
        val useCase = AddWatchLaterEpisodeUseCase(repository, clock)

        useCase(0, "3", "", "", "")
        useCase(42, " ", "", "", "")

        coVerify(exactly = 0) { repository.add(any()) }
    }

    private companion object {
        const val NOW = 1_000_000L
    }
}
