package su.afk.yummy.tv.feature.player.common

import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest

class PlayerBrightnessControllerTest : BaseUnitTest() {

    private val store: PlayerSettingsStore = mockk(relaxed = true)
    private val scope = CoroutineScope(StandardTestDispatcher(testScheduler))

    private fun createController(storedPercent: Int?): PlayerBrightnessController {
        every { store.mobilePlayerBrightnessPercent } returns flowOf(storedPercent)
        coEvery { store.setMobilePlayerBrightnessPercent(any()) } returns Unit
        return PlayerBrightnessController(store, scope)
    }

    @Test
    fun `without stored value brightness stays system`() {
        val controller = createController(storedPercent = null)
        testScheduler.runCurrent()

        assertNull(controller.brightness.value)
    }

    @Test
    fun `stored percent is restored on start`() {
        val controller = createController(storedPercent = 40)
        testScheduler.runCurrent()

        assertEquals(0.4f, controller.brightness.value!!, 0.001f)
    }

    @Test
    fun `gesture value is clamped and persisted after debounce`() {
        val controller = createController(storedPercent = null)
        testScheduler.runCurrent()

        controller.setFraction(0f)
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(600)
        testScheduler.runCurrent()

        assertEquals(0.01f, controller.brightness.value!!, 0.0001f)
        coVerify { store.setMobilePlayerBrightnessPercent(1) }
    }
}
