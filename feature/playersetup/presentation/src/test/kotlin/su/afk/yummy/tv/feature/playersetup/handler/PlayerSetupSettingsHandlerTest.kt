package su.afk.yummy.tv.feature.playersetup.handler

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.playersetup.PlayerSetupState

class PlayerSetupSettingsHandlerTest : BaseUnitTest() {

    private val store: PlayerSettingsStore = mockk(relaxed = true)
    private val accountStore: YaniAccountSettingsStore = mockk(relaxed = true)

    private val showOpening = MutableStateFlow(true)
    private val autoSkip = MutableStateFlow(false)
    private val autoPlay = MutableStateFlow(true)
    private val suggestNext = MutableStateFlow(false)
    private val askDubbing = MutableStateFlow(true)
    private val adaptive = MutableStateFlow(false)
    private val quality = MutableStateFlow(PreferredVideoQuality.P720)
    private val orientation = MutableStateFlow(PlayerOrientationMode.RIGHT)
    private val pip = MutableStateFlow(false)
    private val language = MutableStateFlow(YaniContentLanguage.ENGLISH)

    @Before
    fun setUp() {
        every { store.showOpeningOnTimeline } returns showOpening
        every { store.autoSkipOpeningsEndings } returns autoSkip
        every { store.autoPlayNextEpisode } returns autoPlay
        every { store.suggestNextEpisodeOnWatched } returns suggestNext
        every { store.askDubbingOnWatch } returns askDubbing
        every { store.adaptiveVideoQualityEnabled } returns adaptive
        every { store.preferredVideoQuality } returns quality
        every { store.playerOrientationMode } returns orientation
        every { store.pictureInPictureEnabled } returns pip
        every { accountStore.yaniContentLanguage } returns language
    }

    @Test
    fun `observe merges toggles and playback settings into one state`() = runTest {
        val state = createHandler().observe().first()

        assertEquals(
            PlayerSetupState.State(
                showOpeningOnTimeline = true,
                autoSkipOpeningsEndings = false,
                autoPlayNextEpisode = true,
                suggestNextEpisodeOnWatched = false,
                askDubbingOnWatch = true,
                adaptiveVideoQualityEnabled = false,
                preferredVideoQuality = PreferredVideoQuality.P720,
                playerOrientationMode = PlayerOrientationMode.RIGHT,
                pictureInPictureEnabled = false,
                contentLanguage = YaniContentLanguage.ENGLISH,
            ),
            state,
        )
    }

    @Test
    fun `observe reflects later changes of either group`() = runTest {
        val handler = createHandler()

        autoSkip.value = true
        language.value = YaniContentLanguage.UKRAINIAN
        val state = handler.observe().first()

        assertEquals(true, state.autoSkipOpeningsEndings)
        assertEquals(YaniContentLanguage.UKRAINIAN, state.contentLanguage)
    }

    @Test
    fun `content language is written to the account store, the rest to the player store`() = runTest {
        val handler = createHandler()

        handler.setContentLanguage(YaniContentLanguage.RUSSIAN)
        handler.setPreferredVideoQuality(PreferredVideoQuality.P1080)
        handler.setPlayerOrientationMode(PlayerOrientationMode.LEFT)
        handler.setPictureInPictureEnabled(true)

        coVerify {
            accountStore.setYaniContentLanguage(YaniContentLanguage.RUSSIAN)
            store.setPreferredVideoQuality(PreferredVideoQuality.P1080)
            store.setPlayerOrientationMode(PlayerOrientationMode.LEFT)
            store.setPictureInPictureEnabled(true)
        }
    }

    private fun createHandler() = PlayerSetupSettingsHandler(store = store, accountStore = accountStore)
}
