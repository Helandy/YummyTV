package su.afk.yummy.tv.feature.player.handler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.AllohaPlaybackSessionRepository

class PlayerAllohaSessionHandlerTest : BaseUnitTest() {

    private val repository: AllohaPlaybackSessionRepository = mockk(relaxed = true)

    @Test
    fun `activating a session registers it and the same session is not reactivated`() = runTest {
        val session = session(expiresAt = null)
        val handler = createHandler()

        handler.activate(session, backgroundScope)
        handler.activate(session, backgroundScope)

        verify(exactly = 1) { repository.activate(session) }
    }

    @Test
    fun `a new session releases the previous one`() = runTest {
        val first = session(expiresAt = null)
        val second = session(expiresAt = null)
        val handler = createHandler()

        handler.activate(first, backgroundScope)
        handler.activate(second, backgroundScope)

        verify { repository.release(first, true) }
        verify { repository.activate(second) }
    }

    @Test
    fun `session is refreshed shortly before it expires`() = runTest {
        var expiresAt = System.currentTimeMillis() + 25_000L
        val session = session(expiresAt = null)
        every { session.expiresAtMs() } answers { expiresAt }
        // Ротация публикует новый TTL — иначе цикл ждал бы её до таймаута.
        every { session.refresh() } answers { expiresAt += 60_000L }
        createHandler().activate(session, backgroundScope)

        advanceTimeBy(4_000L)
        runCurrent()
        verify(exactly = 0) { session.refresh() }

        advanceTimeBy(2_000L)
        runCurrent()
        verify(exactly = 1) { session.refresh() }
    }

    @Test
    fun `quality and audio go to the active session`() = runTest {
        val stream: PlayerStreamResolveResult.Stream = mockk()
        val session = session(expiresAt = null)
        every { session.initialStream } returns stream
        val handler = createHandler()
        handler.activate(session, backgroundScope)

        handler.selectQuality("720p")
        val result = handler.selectAudioTrack("a2")

        verify { session.selectQuality("720p") }
        verify { session.selectAudioTrack("a2") }
        assertEquals(stream, result)
    }

    @Test
    fun `without a session there is nothing to switch and close is a no-op`() = runTest {
        val handler = createHandler()

        assertNull(handler.selectAudioTrack("a2"))
        handler.close()

        verify(exactly = 0) { repository.release(any(), any()) }
    }

    @Test
    fun `close releases the session with the requested urgency`() = runTest {
        val session = session(expiresAt = null)
        val handler = createHandler()
        handler.activate(session, backgroundScope)

        handler.close(immediately = false)

        verify { repository.release(session, false) }
    }

    private fun session(expiresAt: Long?): AllohaStreamSession {
        val session: AllohaStreamSession = mockk(relaxed = true)
        every { session.expiresAtMs() } returns expiresAt
        every { repository.activate(session) } returns session
        return session
    }

    private fun createHandler() = PlayerAllohaSessionHandler(repository)
}
