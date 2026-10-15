package su.afk.yummy.tv.feature.details.details.handler

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.feature.player.IPlayerNavigator

class DetailsPlayerNavigationHandlerTest : BaseUnitTest() {

    private val navigator: IPlayerNavigator = mockk()
    private val video = AnimeVideo(
        id = 1, episode = "1", dubbing = "d", player = "p", playerId = null, iframeUrl = "u", durationSeconds = null,
    )

    private fun extra(answer: Boolean): ExtraVideoRepository = mockk<ExtraVideoRepository>().also {
        coEvery { it.confirmOpen(video) } returns answer
    }

    @Test
    fun `no extra sources never blocks opening`() = runTest {
        assertEquals(true, DetailsPlayerNavigationHandler(navigator).confirmOpen(video))
    }

    @Test
    fun `any extra source can veto the opening`() = runTest {
        val handler = DetailsPlayerNavigationHandler(navigator, setOf(extra(true), extra(false)))

        assertEquals(false, handler.confirmOpen(video))
    }

    @Test
    fun `all extra sources agreeing allows the opening`() = runTest {
        val handler = DetailsPlayerNavigationHandler(navigator, setOf(extra(true), extra(true)))

        assertEquals(true, handler.confirmOpen(video))
    }
}
