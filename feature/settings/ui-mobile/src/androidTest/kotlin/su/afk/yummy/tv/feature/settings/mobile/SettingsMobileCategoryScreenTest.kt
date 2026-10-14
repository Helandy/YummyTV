package su.afk.yummy.tv.feature.settings.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.model.titleRes
import su.afk.yummy.tv.feature.settings.navigator.SettingsCategory
import su.afk.yummy.tv.feature.settings.presentation.R

class SettingsMobileCategoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<SettingsState.Event>()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(category: SettingsCategory, state: SettingsState.State = SettingsState.State()) {
        composeRule.setContent {
            ScreenPreviewTheme {
                SettingsMobileCategoryScreen(
                    category = category,
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    private fun assertTitleIsShown(category: SettingsCategory) {
        setScreen(category)

        composeRule.onNodeWithText(string(category.titleRes)).assertIsDisplayed()
    }

    @Test
    fun generalShowsTitle() = assertTitleIsShown(SettingsCategory.GENERAL)

    @Test
    fun appearanceShowsTitle() = assertTitleIsShown(SettingsCategory.APPEARANCE)

    @Test
    fun playerShowsTitle() = assertTitleIsShown(SettingsCategory.PLAYER)

    @Test
    fun playbackShowsTitle() = assertTitleIsShown(SettingsCategory.PLAYBACK)

    @Test
    fun watchProgressShowsTitle() = assertTitleIsShown(SettingsCategory.WATCH_PROGRESS)

    @Test
    fun subtitlesShowsTitle() = assertTitleIsShown(SettingsCategory.SUBTITLES)

    @Test
    fun storageShowsTitle() = assertTitleIsShown(SettingsCategory.STORAGE)

    @Test
    fun apiShowsTitle() = assertTitleIsShown(SettingsCategory.API)

    @Test
    fun playbackAskDubbingToggleSendsEvent() {
        setScreen(SettingsCategory.PLAYBACK)

        composeRule.onNodeWithText(string(R.string.settings_ask_dubbing_on_watch_label)).performClick()

        assertTrue(SettingsState.Event.AskDubbingOnWatchToggled in events)
    }

    @Test
    fun playbackAutoSkipToggleSendsEvent() {
        setScreen(SettingsCategory.PLAYBACK)

        composeRule.onNodeWithText(string(R.string.settings_auto_skip_label)).performScrollTo().performClick()

        assertTrue(SettingsState.Event.AutoSkipOpeningsEndingsToggled in events)
    }

    @Test
    fun playbackAutoPlayToggleSendsEvent() {
        setScreen(SettingsCategory.PLAYBACK)

        composeRule.onNodeWithText(string(R.string.settings_auto_play_next_episode_label)).performScrollTo().performClick()

        assertTrue(SettingsState.Event.AutoPlayNextEpisodeToggled in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(SettingsCategory.GENERAL)

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(SettingsState.Event.BackSelected in events)
    }
}
