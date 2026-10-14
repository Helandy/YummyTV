package su.afk.yummy.tv.feature.settings.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.model.titleRes
import su.afk.yummy.tv.feature.settings.navigator.SettingsCategory
import su.afk.yummy.tv.feature.settings.presentation.R

class SettingsMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<SettingsState.Event>()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: SettingsState.State = SettingsState.State()) {
        composeRule.setContent {
            ScreenPreviewTheme {
                SettingsMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleAndCategories() {
        setScreen()

        composeRule.onNodeWithText(string(R.string.settings_mobile_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(SettingsCategory.entries.first().titleRes)).assertIsDisplayed()
    }

    @Test
    fun categoryClickSendsCategorySelected() {
        setScreen()
        val category = SettingsCategory.entries.first()

        composeRule.onNodeWithText(string(category.titleRes)).performClick()

        assertEquals(listOf<SettingsState.Event>(SettingsState.Event.CategorySelected(category)), events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen()

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertEquals(listOf<SettingsState.Event>(SettingsState.Event.BackSelected), events)
    }

    @Test
    fun releaseNotesClickRequestsReleaseNotes() {
        setScreen()

        composeRule.onNodeWithText(string(R.string.settings_release_notes_label)).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(SettingsState.Event.ReleaseNotesRequested in events)
    }

    @Test
    fun shareLogsRowIsHiddenUntilRecordingEnabled() {
        setScreen(SettingsState.State(appLogRecordingEnabled = false))

        composeRule.onNodeWithText(string(R.string.settings_release_notes_label)).performScrollTo()
        composeRule.onNodeWithText(string(R.string.settings_logs_label)).assertDoesNotExist()
    }

    @Test
    fun shareLogsRowSendsShareLogsClicked() {
        setScreen(SettingsState.State(appLogRecordingEnabled = true))

        composeRule.onNodeWithText(string(R.string.settings_logs_label)).performScrollTo().performClick()

        assertEquals(listOf<SettingsState.Event>(SettingsState.Event.ShareLogsClicked), events)
    }

    @Test
    fun analyticsIdRowIsShownOnlyWhenIdIsKnown() {
        setScreen(SettingsState.State(analyticsDeviceId = "123 456 789"))

        composeRule.onNodeWithText(string(R.string.settings_analytics_id_label)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("123 456 789").assertIsDisplayed()
    }
}
