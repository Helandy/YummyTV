package su.afk.yummy.tv.feature.top.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.feature.top.TopState
import su.afk.yummy.tv.feature.top.presentation.R

class TopMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<TopState.Event>()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: TopState.State = TopState.State()) {
        composeRule.setContent {
            ScreenPreviewTheme {
                TopMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsAllTypeTabs() {
        setScreen()

        composeRule.onNodeWithText(string(R.string.top_type_tv)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.top_type_movie)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.top_type_ona)).assertIsDisplayed()
    }

    @Test
    fun movieTabSendsTypeSelected() {
        setScreen()

        composeRule.onNodeWithText(string(R.string.top_type_movie)).performClick()
        composeRule.waitForIdle()

        assertTrue(TopState.Event.TypeSelected(AnimeTopType.MOVIE) in events)
    }
}
