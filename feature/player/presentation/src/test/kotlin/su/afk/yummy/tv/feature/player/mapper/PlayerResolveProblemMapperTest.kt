package su.afk.yummy.tv.feature.player.mapper

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.core.common.player.ResolveDetail
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.feature.player.presentation.R

class PlayerResolveProblemMapperTest {

    private val strings = mockk<StringProvider> {
        every { get(any<Int>()) } answers { "res:${firstArg<Int>()}" }
        every { get(any<Int>(), *anyVararg()) } answers { "res:${firstArg<Int>()}:${args.drop(1).flatMap { (it as Array<*>).toList() }}" }
    }

    private fun detail(code: Int? = null, error: String? = null) = ResolveDetail("kodik.info", code, error, 100)

    @Test
    fun `server error code becomes http problem with host and code`() {
        val text = resolveProblemText(strings, PlayerStreamResolveStatus.ResponseError, listOf(detail(code = 503)))
        assertEquals("res:${R.string.player_problem_http_server}:[kodik.info, 503]", text)
    }

    @Test
    fun `timeout becomes network problem`() {
        val text = resolveProblemText(strings, PlayerStreamResolveStatus.FetchingPage, listOf(detail(error = "SocketTimeoutException")))
        assertEquals("res:${R.string.player_problem_network_timeout}:[kodik.info]", text)
    }

    @Test
    fun `successful response then parsing step means parse problem`() {
        val text = resolveProblemText(strings, PlayerStreamResolveStatus.ParsingPage, listOf(detail(code = 200)))
        assertEquals("res:${R.string.player_problem_parse}", text)
    }

    @Test
    fun `no information gives null`() {
        assertNull(resolveProblemText(strings, PlayerStreamResolveStatus.Preparing, emptyList()))
    }

    @Test
    fun `terminal misses give specific problems`() {
        assertEquals(
            "res:${R.string.player_problem_episode_not_found}",
            resolveProblemText(strings, PlayerStreamResolveStatus.EpisodeNotFound, listOf(detail(code = 200))),
        )
        assertEquals(
            "res:${R.string.player_problem_link_not_found}",
            resolveProblemText(strings, PlayerStreamResolveStatus.LinkNotFound, listOf(detail(code = 200))),
        )
    }
}
