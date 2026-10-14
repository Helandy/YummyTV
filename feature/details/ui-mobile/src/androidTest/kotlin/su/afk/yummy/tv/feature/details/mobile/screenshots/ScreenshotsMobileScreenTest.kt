package su.afk.yummy.tv.feature.details.mobile.screenshots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.core.model.anime.AnimeScreenshot
import su.afk.yummy.tv.feature.details.screenshots.ScreenshotsState

class ScreenshotsMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<ScreenshotsState.Event>()

    private fun string(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun setScreen(state: ScreenshotsState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                ScreenshotsMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun blankTitleFallsBackToDefault() {
        setScreen(ScreenshotsState.State(isLoading = false))

        composeRule.onNodeWithText(string(R.string.details_screenshots_title)).assertIsDisplayed()
    }

    @Test
    fun customTitleIsShown() {
        setScreen(ScreenshotsState.State(isLoading = false, title = "Тестовое аниме"))

        composeRule.onNodeWithText("Тестовое аниме").assertIsDisplayed()
    }

    @Test
    fun screenshotClickSendsScreenshotSelectedWithIndex() {
        setScreen(
            ScreenshotsState.State(
                isLoading = false,
                screenshots = persistentListOf(
                    AnimeScreenshot(id = 1, episode = "3", small = null, full = null),
                    AnimeScreenshot(id = 2, episode = "4", small = null, full = null),
                ),
            ),
        )

        composeRule.onNodeWithContentDescription(string(R.string.details_episode_number, "4")).performClick()

        assertEquals(listOf<ScreenshotsState.Event>(ScreenshotsState.Event.ScreenshotSelected(1)), events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(ScreenshotsState.State(isLoading = false))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertEquals(listOf<ScreenshotsState.Event>(ScreenshotsState.Event.BackSelected), events)
    }
}
