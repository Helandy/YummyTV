package su.afk.yummy.tv.feature.player.utils

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.model.PlayerStreamUnavailableCause
import su.afk.yummy.tv.feature.player.presentation.R

class PlayerStreamUtilsTest : BaseUnitTest() {

    private val strings: StringProvider = mockk()

    @Before
    fun setUp() {
        every { strings.get(any<Int>()) } answers { "res${firstArg<Int>()}" }
    }

    private fun message(result: PlayerStreamResolveResult.Unavailable) = result.toMessage(strings)

    @Test
    fun `each unavailable cause maps to its own string`() {
        assertEquals(
            "res${R.string.player_video_not_found}",
            message(PlayerStreamResolveResult.Unavailable(cause = PlayerStreamUnavailableCause.VideoNotFound)),
        )
        assertEquals(
            "res${R.string.player_access_forbidden}",
            message(PlayerStreamResolveResult.Unavailable(cause = PlayerStreamUnavailableCause.AccessForbidden)),
        )
        assertEquals(
            "res${R.string.player_region_blocked}",
            message(PlayerStreamResolveResult.Unavailable(cause = PlayerStreamUnavailableCause.RegionBlocked)),
        )
    }

    @Test
    fun `source message wins over the cause`() {
        val result = PlayerStreamResolveResult.Unavailable(
            message = "Видео доступно только друзьям.",
            cause = PlayerStreamUnavailableCause.VideoNotFound,
        )

        assertEquals("Видео доступно только друзьям.", message(result))
    }

    @Test
    fun `no cause and no message falls back to the generic dubbing message`() {
        assertEquals(
            "res${R.string.player_dubbing_unavailable}",
            message(PlayerStreamResolveResult.Unavailable()),
        )
    }
}
