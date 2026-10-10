package su.afk.yummy.tv.data.player.session

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession

/** Менеджер живой Alloha-сессии: смена конфигурации держит сессию 10 секунд до закрытия. */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAllohaPlaybackSessionManagerTest : BaseUnitTest() {

    private fun session(key: String = "source-1"): AllohaStreamSession =
        mockk<AllohaStreamSession>(relaxed = true).also { every { it.sourceKey } returns key }

    @Test
    fun `find returns the active session of the same source only`() = runTest {
        val manager = DefaultAllohaPlaybackSessionManager(backgroundScope)
        val active = manager.activate(session("a"))

        assertSame(active, manager.find("a"))
        assertNull(manager.find("b"))
    }

    @Test
    fun `activating another session closes the previous one`() = runTest {
        val manager = DefaultAllohaPlaybackSessionManager(backgroundScope)
        val first = manager.activate(session("a"))

        manager.activate(session("b"))

        verify { first.close() }
        assertNull(manager.find("a"))
    }

    @Test
    fun `activating the same session again keeps it open`() = runTest {
        val manager = DefaultAllohaPlaybackSessionManager(backgroundScope)
        val active = manager.activate(session("a"))

        manager.activate(active)

        verify(exactly = 0) { active.close() }
        assertSame(active, manager.find("a"))
    }

    @Test
    fun `immediate release closes the active session`() = runTest {
        val manager = DefaultAllohaPlaybackSessionManager(backgroundScope)
        val active = manager.activate(session("a"))

        manager.release(active, immediately = true)

        verify(exactly = 1) { active.close() }
        assertNull(manager.find("a"))
    }

    @Test
    fun `releasing a session that is not active closes it right away`() = runTest {
        val manager = DefaultAllohaPlaybackSessionManager(backgroundScope)
        manager.activate(session("a"))
        val stranger = session("b")

        manager.release(stranger, immediately = false)

        verify(exactly = 1) { stranger.close() }
    }

    @Test
    fun `session stays open just before the grace period ends`() = runTest {
        val manager = DefaultAllohaPlaybackSessionManager(backgroundScope)
        val active = manager.activate(session("a"))

        manager.release(active, immediately = false)
        runCurrent()
        advanceTimeBy(GRACE_MS - 1)
        runCurrent()

        verify(exactly = 0) { active.close() }
    }

    @Test
    fun `delayed release closes the session after ten seconds`() = runTest {
        val manager = DefaultAllohaPlaybackSessionManager(backgroundScope)
        val active = manager.activate(session("a"))

        manager.release(active, immediately = false)
        runCurrent()
        advanceTimeBy(GRACE_MS)
        runCurrent()

        verify(exactly = 1) { active.close() }
        assertNull(manager.find("a"))
    }

    @Test
    fun `find during the grace period cancels the release`() = runTest {
        val manager = DefaultAllohaPlaybackSessionManager(backgroundScope)
        val active = manager.activate(session("a"))
        manager.release(active, immediately = false)
        runCurrent()

        advanceTimeBy(GRACE_MS / 2)
        manager.find("a")
        advanceTimeBy(GRACE_MS)
        runCurrent()

        verify(exactly = 0) { active.close() }
    }

    @Test
    fun `activate during the grace period cancels the release`() = runTest {
        val manager = DefaultAllohaPlaybackSessionManager(backgroundScope)
        val active = manager.activate(session("a"))
        manager.release(active, immediately = false)
        runCurrent()

        manager.activate(active)
        advanceTimeBy(GRACE_MS * 2)
        runCurrent()

        verify(exactly = 0) { active.close() }
    }

    @Test
    fun `second delayed release restarts the grace period`() = runTest {
        val manager = DefaultAllohaPlaybackSessionManager(backgroundScope)
        val active = manager.activate(session("a"))
        manager.release(active, immediately = false)
        runCurrent()
        advanceTimeBy(GRACE_MS - 1_000)

        manager.release(active, immediately = false)
        runCurrent()
        advanceTimeBy(GRACE_MS - 1)
        runCurrent()
        verify(exactly = 0) { active.close() }

        advanceTimeBy(1)
        runCurrent()
        verify(exactly = 1) { active.close() }
    }

    @Test
    fun `close active shuts the session and cancels the pending release`() = runTest {
        val manager = DefaultAllohaPlaybackSessionManager(backgroundScope)
        val active = manager.activate(session("a"))
        manager.release(active, immediately = false)
        runCurrent()

        manager.closeActive()
        advanceTimeBy(GRACE_MS * 2)
        runCurrent()

        verify(exactly = 1) { active.close() }
        assertNull(manager.find("a"))
    }

    private companion object {
        const val GRACE_MS = 10_000L
    }
}
