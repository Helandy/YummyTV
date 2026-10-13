package su.afk.yummy.tv.feature.player.handler

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.PlayerMobileVideoTransformSettings
import su.afk.yummy.tv.core.model.settings.PlayerResizeMode
import su.afk.yummy.tv.core.model.settings.PlayerResizeSettings
import su.afk.yummy.tv.core.model.settings.PlayerZoomLevel
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.player.PlayerSourceSelection
import su.afk.yummy.tv.feature.player.behavior.PlayerSourceHostMock
import su.afk.yummy.tv.feature.player.behavior.onlineState

class PlayerDisplaySettingsHandlerTest : BaseUnitTest() {

    private val store: PlayerSettingsStore = mockk(relaxed = true)

    private val kodikResize = MutableStateFlow(PlayerResizeSettings(PlayerResizeMode.CROP, PlayerZoomLevel.PERCENT_20))
    private val kodikTransform = MutableStateFlow(PlayerMobileVideoTransformSettings(scale = 1.5f, offsetX = 3f))

    @Before
    fun setUp() {
        every { store.playerResizeSettings(ANIME_ID, any(), KODIK) } returns kodikResize
        every { store.playerMobileVideoTransformSettings(ANIME_ID, any(), KODIK) } returns kodikTransform
    }

    @Test
    fun `stored settings of the active title and player are applied`() = runTest {
        val host = host(backgroundScope)

        createHandler().observeActive(host.host)
        runCurrent()

        assertEquals(PlayerResizeMode.CROP, host.state.resizeMode)
        assertEquals(PlayerZoomLevel.PERCENT_20, host.state.zoomLevel)
        assertEquals(1.5f, host.state.mobileVideoScale)
        assertEquals(3f, host.state.mobileVideoOffsetX)
    }

    @Test
    fun `the same scope is observed once unless forced`() = runTest {
        val host = host(backgroundScope)
        val handler = createHandler()

        handler.observeActive(host.host)
        handler.observeActive(host.host)
        handler.observeActive(host.host, force = true)

        verify(exactly = 2) { store.playerResizeSettings(ANIME_ID, any(), KODIK) }
    }

    @Test
    fun `defaults are shown until the new scope delivers its settings`() = runTest {
        val host = host(backgroundScope)
        val handler = createHandler()
        handler.observeActive(host.host)
        runCurrent()
        // Новый балансер: его настройки ещё не пришли.
        every { store.playerResizeSettings(ANIME_ID, any(), "Other") } returns MutableStateFlow(PlayerResizeSettings())
        every { store.playerMobileVideoTransformSettings(ANIME_ID, any(), "Other") } returns
            MutableStateFlow(PlayerMobileVideoTransformSettings())
        host.update { copy(sourceGraph = sourceGraph.copy(balancers = sourceGraph.balancers.map { it.copy(name = "Other") })) }

        handler.observeActive(host.host)
        kodikResize.value = PlayerResizeSettings(PlayerResizeMode.STRETCH)
        runCurrent()

        assertEquals(PlayerResizeMode.FIT, host.state.resizeMode)
        assertEquals(1f, host.state.mobileVideoScale)
    }

    @Test
    fun `resize mode keeps the zoom and is saved for the scope`() = runTest {
        val host = host(backgroundScope)
        host.update { copy(zoomLevel = PlayerZoomLevel.PERCENT_15) }

        createHandler().selectResizeMode(host.host, PlayerResizeMode.STRETCH)
        runCurrent()

        assertEquals(PlayerResizeMode.STRETCH, host.state.resizeMode)
        coVerify {
            store.setPlayerResizeSettings(
                ANIME_ID,
                any(),
                KODIK,
                PlayerResizeSettings(PlayerResizeMode.STRETCH, PlayerZoomLevel.PERCENT_15),
            )
        }
    }

    @Test
    fun `zoom level switches the mode to zoom`() = runTest {
        val host = host(backgroundScope)

        createHandler().selectZoomLevel(host.host, PlayerZoomLevel.PERCENT_25)
        runCurrent()

        assertEquals(PlayerResizeMode.ZOOM, host.state.resizeMode)
        assertEquals(PlayerZoomLevel.PERCENT_25, host.state.zoomLevel)
        coVerify {
            store.setPlayerResizeSettings(
                ANIME_ID,
                any(),
                KODIK,
                PlayerResizeSettings(PlayerResizeMode.ZOOM, PlayerZoomLevel.PERCENT_25),
            )
        }
    }

    @Test
    fun `mobile transform is shown at once and only the last one is saved`() = runTest {
        val host = host(backgroundScope)
        val handler = createHandler()
        val first = PlayerMobileVideoTransformSettings(scale = 1.2f)
        val last = PlayerMobileVideoTransformSettings(scale = 2f, offsetX = 5f, offsetY = -5f)

        handler.changeMobileTransform(host.host, first)
        advanceTimeBy(100)
        handler.changeMobileTransform(host.host, last)

        assertEquals(2f, host.state.mobileVideoScale)
        assertEquals(-5f, host.state.mobileVideoOffsetY)
        // Запись отложена debounce'ом во фоне: advanceUntilIdle фоновые задачи не ждёт.
        advanceTimeBy(SAVE_DEBOUNCE_MS)
        runCurrent()
        coVerify(exactly = 0) { store.setPlayerMobileVideoTransformSettings(any(), any(), any(), first) }
        coVerify(exactly = 1) { store.setPlayerMobileVideoTransformSettings(ANIME_ID, any(), KODIK, last) }
    }

    private fun TestScope.host(scope: CoroutineScope) =
        PlayerSourceHostMock(scope, onlineState(KODIK).copy(sourceSelection = PlayerSourceSelection()))

    private fun createHandler() = PlayerDisplaySettingsHandler(
        settings = PlayerSettingsHandler(store),
        sourceSelection = PlayerSourceSelectionHandler(),
    )

    private companion object {
        const val ANIME_ID = 42
        const val KODIK = "Kodik"
        const val SAVE_DEBOUNCE_MS = 300L
    }
}
