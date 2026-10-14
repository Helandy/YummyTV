package su.afk.yummy.tv.feature.playersetup.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.feature.playersetup.PlayerSetupState
import su.afk.yummy.tv.feature.playersetup.presentation.R

class PlayerSetupMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<PlayerSetupState.Event>()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: PlayerSetupState.State = PlayerSetupState.State()) {
        composeRule.setContent {
            ScreenPreviewTheme {
                PlayerSetupMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    private fun clickRow(labelRes: Int) {
        composeRule.onNodeWithText(string(labelRes)).performScrollTo().performClick()
    }

    @Test
    fun showsTitleAndSubtitle() {
        setScreen()

        composeRule.onNodeWithText(string(R.string.player_setup_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.player_setup_subtitle)).assertIsDisplayed()
    }

    @Test
    fun openingTimelineToggleSendsEvent() {
        setScreen()

        clickRow(R.string.player_setup_opening_timeline_label)

        assertTrue(PlayerSetupState.Event.ShowOpeningOnTimelineToggled in events)
    }

    @Test
    fun autoSkipToggleSendsEvent() {
        setScreen()

        clickRow(R.string.player_setup_auto_skip_label)

        assertTrue(PlayerSetupState.Event.AutoSkipOpeningsEndingsToggled in events)
    }

    @Test
    fun autoPlayToggleSendsEvent() {
        setScreen()

        clickRow(R.string.player_setup_auto_play_label)

        assertTrue(PlayerSetupState.Event.AutoPlayNextEpisodeToggled in events)
    }

    @Test
    fun suggestNextToggleSendsEvent() {
        setScreen()

        clickRow(R.string.player_setup_suggest_next_label)

        assertTrue(PlayerSetupState.Event.SuggestNextEpisodeOnWatchedToggled in events)
    }

    @Test
    fun askDubbingToggleSendsEvent() {
        setScreen()

        clickRow(R.string.player_setup_ask_dubbing_label)

        assertTrue(PlayerSetupState.Event.AskDubbingOnWatchToggled in events)
    }

    @Test
    fun pictureInPictureToggleSendsEvent() {
        setScreen()

        clickRow(R.string.player_setup_mobile_pip_label)

        assertTrue(PlayerSetupState.Event.PictureInPictureToggled in events)
    }

    @Test
    fun refreshProgressToggleSendsEvent() {
        setScreen()

        clickRow(R.string.player_setup_refresh_progress_label)

        assertTrue(PlayerSetupState.Event.RefreshContinueWatchingProgressToggled in events)
    }

    @Test
    fun qualityPickerSendsPreferredVideoQualitySelected() {
        setScreen()

        clickRow(R.string.player_setup_quality_label)
        // лист выбора открыт: значение «Best» есть и в строке, и в списке вариантов
        composeRule.onAllNodesWithText(string(R.string.player_setup_quality_best)).onLast().performClick()

        assertTrue(PlayerSetupState.Event.PreferredVideoQualitySelected(PreferredVideoQuality.BEST) in events)
    }

    @Test
    fun doneButtonSendsDoneSelected() {
        setScreen()

        composeRule.onNodeWithText(string(DesignSystemR.string.common_done)).performScrollTo().performClick()

        assertTrue(PlayerSetupState.Event.DoneSelected in events)
    }
}
