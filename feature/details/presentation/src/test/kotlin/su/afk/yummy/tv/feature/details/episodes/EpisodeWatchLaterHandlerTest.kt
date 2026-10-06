package su.afk.yummy.tv.feature.details.episodes

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.utils.coroutines.AppClock
import su.afk.yummy.tv.domain.watchlater.repository.WatchLaterRepository
import su.afk.yummy.tv.domain.watchlater.usecase.AddWatchLaterEpisodeUseCase
import su.afk.yummy.tv.domain.watchlater.usecase.RemoveWatchLaterEpisodeUseCase
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeWatchLaterHandler
import su.afk.yummy.tv.feature.details.episodes.handler.EpisodeWatchedHandler

/** Пометка «отложить просмотр» — переключатель, сервера у него нет. */
class EpisodeWatchLaterHandlerTest : BaseUnitTest() {

    private val repository: WatchLaterRepository = mockk(relaxed = true)
    private lateinit var clock: AppClock

    @Before
    fun setUp() {
        clock = mockk<AppClock> { every { nowMillis() } returns NOW }
    }

    private fun createHandler() = EpisodeWatchLaterHandler(
        addWatchLaterEpisode = AddWatchLaterEpisodeUseCase(repository, clock),
        removeWatchLaterEpisode = RemoveWatchLaterEpisodeUseCase(repository),
    )

    private val meta = EpisodeWatchedHandler.EpisodeMeta(
        animeTitle = "Title",
        posterUrl = "poster",
        screenshotUrl = "shot",
    )

    @Test
    fun `adds episode with title metadata when it is not postponed yet`() = runTest {
        createHandler().toggle(animeId = 7, episode = "3", isInWatchLater = false, meta = meta)

        coVerify(exactly = 1) {
            repository.add(
                withArg {
                    assertEquals(7, it.animeId)
                    assertEquals("3", it.episode)
                    assertEquals("Title", it.animeTitle)
                    assertEquals("shot", it.screenshotUrl)
                    assertEquals(NOW, it.addedAt)
                },
            )
        }
        coVerify(exactly = 0) { repository.remove(any(), any()) }
    }

    @Test
    fun `removes episode when it is already postponed`() = runTest {
        createHandler().toggle(animeId = 7, episode = "3", isInWatchLater = true, meta = meta)

        coVerify(exactly = 1) { repository.remove(7, "3") }
        coVerify(exactly = 0) { repository.add(any()) }
    }

    private companion object {
        const val NOW = 1_000L
    }
}
