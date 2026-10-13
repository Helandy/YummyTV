package su.afk.yummy.tv.feature.details.episodes.handler

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.watchlater.usecase.AddWatchLaterEpisodeUseCase
import su.afk.yummy.tv.domain.watchlater.usecase.RemoveWatchLaterEpisodeUseCase

class EpisodeWatchLaterHandlerTest : BaseUnitTest() {

    private val add: AddWatchLaterEpisodeUseCase = mockk(relaxed = true)
    private val remove: RemoveWatchLaterEpisodeUseCase = mockk(relaxed = true)
    private val meta = EpisodeWatchedHandler.EpisodeMeta("Title", "poster", "shot")

    @Test
    fun `toggle adds a new episode with its card metadata`() = runTest {
        EpisodeWatchLaterHandler(add, remove).toggle(ANIME_ID, "3", isInWatchLater = false, meta = meta)

        coVerify { add(ANIME_ID, "3", "Title", "poster", "shot") }
        coVerify(exactly = 0) { remove(any(), any()) }
    }

    @Test
    fun `toggle removes an already postponed episode`() = runTest {
        EpisodeWatchLaterHandler(add, remove).toggle(ANIME_ID, "3", isInWatchLater = true, meta = meta)

        coVerify { remove(ANIME_ID, "3") }
        coVerify(exactly = 0) { add(any(), any(), any(), any(), any()) }
    }

    private companion object {
        const val ANIME_ID = 42
    }
}
