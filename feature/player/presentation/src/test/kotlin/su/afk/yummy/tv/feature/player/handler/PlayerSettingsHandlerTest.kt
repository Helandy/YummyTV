package su.afk.yummy.tv.feature.player.handler

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.PlayerMobileVideoTransformSettings
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.model.settings.PlayerResizeSettings
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleStyleSettings
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleTextColor
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.player.model.PlayerResizeSettingsScope

class PlayerSettingsHandlerTest : BaseUnitTest() {

    private val store: PlayerSettingsStore = mockk(relaxed = true)
    private val scope = PlayerResizeSettingsScope(animeId = 42, animeTitle = "Title", playerName = "Kodik")

    @Test
    fun `resize settings are read and written for the title and player`() = runTest {
        val stored = PlayerResizeSettings(resizeMode = PlayerResizeMode.CROP)
        every { store.playerResizeSettings(42, "Title", "Kodik") } returns flowOf(stored)
        val handler = PlayerSettingsHandler(store)

        assertEquals(stored, handler.observeResizeSettings(scope).first())
        handler.saveResizeSettings(scope, stored)

        coVerify { store.setPlayerResizeSettings(42, "Title", "Kodik", stored) }
    }

    @Test
    fun `mobile transform is written only after the debounce`() = runTest {
        val value = PlayerMobileVideoTransformSettings(scale = 1.4f)
        val handler = PlayerSettingsHandler(store)

        launch { handler.saveMobileVideoTransformSettings(scope, value) }
        advanceTimeBy(200)
        runCurrent()
        coVerify(exactly = 0) { store.setPlayerMobileVideoTransformSettings(any(), any(), any(), any()) }

        advanceTimeBy(100)
        runCurrent()
        coVerify { store.setPlayerMobileVideoTransformSettings(42, "Title", "Kodik", value) }
    }

    @Test
    fun `subtitle style and volume stabilization are written to the shared store`() = runTest {
        val style = PlayerSubtitleStyleSettings(textColor = PlayerSubtitleTextColor.YELLOW)
        val handler = PlayerSettingsHandler(store)

        handler.saveSubtitleStyle(style)
        handler.saveVolumeStabilizationEnabled(true)

        coVerify { store.setPlayerSubtitleStyle(style) }
        coVerify { store.setVolumeStabilizationEnabled(true) }
    }
}
