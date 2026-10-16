package su.afk.yummy.tv.feature.playersetup

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.playersetup.PlayerSetupState.Event
import su.afk.yummy.tv.feature.playersetup.handler.PlayerSetupSettingsHandler

class PlayerSetupViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val settingsHandler: PlayerSetupSettingsHandler = mockk(relaxed = true)

    private val settings = MutableStateFlow(PlayerSetupState.State())

    @Before
    fun setUp() {
        every { settingsHandler.observe() } returns settings
    }

    @Test
    fun `state mirrors the stored settings`() {
        val viewModel = createViewModel()
        val stored = PlayerSetupState.State(
            autoSkipOpeningsEndings = true,
            preferredVideoQuality = PreferredVideoQuality.P1080,
            contentLanguage = YaniContentLanguage.ENGLISH,
        )

        settings.value = stored

        assertEquals(stored, viewModel.state.value)
    }

    @Test
    fun `toggles write the inverse of the current value`() {
        settings.value = PlayerSetupState.State(
            showOpeningOnTimeline = false,
            autoSkipOpeningsEndings = true,
            autoPlayNextEpisode = false,
            suggestNextEpisodeOnWatched = true,
            askDubbingOnWatch = true,
            pictureInPictureEnabled = false,
        )
        val viewModel = createViewModel()

        viewModel.setEvent(Event.ShowOpeningOnTimelineToggled)
        viewModel.setEvent(Event.AutoSkipOpeningsEndingsToggled)
        viewModel.setEvent(Event.AutoPlayNextEpisodeToggled)
        viewModel.setEvent(Event.SuggestNextEpisodeOnWatchedToggled)
        viewModel.setEvent(Event.AskDubbingOnWatchToggled)
        viewModel.setEvent(Event.PictureInPictureToggled)

        coVerify {
            settingsHandler.setShowOpeningOnTimeline(true)
            settingsHandler.setAutoSkipOpeningsEndings(false)
            settingsHandler.setAutoPlayNextEpisode(true)
            settingsHandler.setSuggestNextEpisodeOnWatched(false)
            settingsHandler.setAskDubbingOnWatch(false)
            settingsHandler.setPictureInPictureEnabled(true)
        }
    }

    @Test
    fun `selections write the chosen value`() {
        val viewModel = createViewModel()

        viewModel.setEvent(Event.ContentLanguageSelected(YaniContentLanguage.UKRAINIAN))
        viewModel.setEvent(Event.PreferredVideoQualitySelected(PreferredVideoQuality.P1440))
        viewModel.setEvent(Event.PlayerOrientationModeSelected(PlayerOrientationMode.LEFT))

        coVerify {
            settingsHandler.setContentLanguage(YaniContentLanguage.UKRAINIAN)
            settingsHandler.setPreferredVideoQuality(PreferredVideoQuality.P1440)
            settingsHandler.setPlayerOrientationMode(PlayerOrientationMode.LEFT)
        }
    }

    @Test
    fun `done closes the setup screen`() {
        val viewModel = createViewModel()

        viewModel.setEvent(Event.DoneSelected)

        verify { nav.back() }
    }

    private fun createViewModel() = PlayerSetupViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        settingsHandler = settingsHandler,
    )
}
