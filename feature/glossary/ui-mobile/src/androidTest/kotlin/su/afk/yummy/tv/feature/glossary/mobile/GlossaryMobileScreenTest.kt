package su.afk.yummy.tv.feature.glossary.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.glossary.GlossaryState
import su.afk.yummy.tv.feature.glossary.model.GlossaryCategory
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.feature.glossary.presentation.R

class GlossaryMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<GlossaryState.Event>()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: GlossaryState.State = GlossaryState.State()) {
        composeRule.setContent {
            ScreenPreviewTheme {
                GlossaryMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleAndFirstTerm() {
        setScreen()

        composeRule.onNodeWithText(string(R.string.glossary_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.glossary_title_term)).assertIsDisplayed()
    }

    @Test
    fun categoryHeaderClickSendsToggleEvent() {
        setScreen()

        composeRule.onNodeWithText(string(R.string.glossary_category_slang)).performClick()

        assertEquals(listOf(GlossaryState.Event.CategoryToggled(GlossaryCategory.SLANG)), events)
    }

    @Test
    fun noMatchesShowsEmptyMessage() {
        setScreen(GlossaryState.State(query = "zzzzzz"))

        composeRule.onNodeWithText(string(R.string.glossary_empty)).assertIsDisplayed()
    }

    @Test
    fun backButtonSendsBackEvent() {
        setScreen()

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertEquals(listOf<GlossaryState.Event>(GlossaryState.Event.BackSelected), events)
    }
}
