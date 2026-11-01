package su.afk.yummy.tv.feature.player.host

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.player.PlayerState

class PlayerChangePlayerHintTest : BaseUnitTest() {

    private var state = PlayerState.State()
    private var offline = false

    private fun createHint(scope: CoroutineScope) =
        PlayerChangePlayerHint(
            scope = scope,
            update = { reducer -> state = state.reducer() },
            isOffline = { offline },
        )

    @Test
    fun `подсказка появляется через задержку`() = runTest {
        createHint(backgroundScope).start(10_000L)

        advanceTimeBy(9_999L)
        assertFalse(state.showChangePlayerHint)

        advanceTimeBy(2L)
        assertTrue(state.showChangePlayerHint)
    }

    @Test
    fun `без сети подсказка откладывается до её возвращения`() = runTest {
        offline = true
        createHint(backgroundScope).start(10_000L)

        advanceTimeBy(35_000L)
        assertFalse(state.showChangePlayerHint)

        offline = false
        advanceTimeBy(10_000L)
        runCurrent()
        assertTrue(state.showChangePlayerHint)
    }

    @Test
    fun `подсказка не появляется, если восстановление уже закончилось`() = runTest {
        var recovering = true
        createHint(backgroundScope).start(10_000L) { recovering }

        recovering = false
        advanceTimeBy(11_000L)

        assertFalse(state.showChangePlayerHint)
    }
}
