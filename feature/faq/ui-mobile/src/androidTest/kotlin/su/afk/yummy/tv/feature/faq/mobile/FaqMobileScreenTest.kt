package su.afk.yummy.tv.feature.faq.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.faq.FaqState
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.feature.faq.presentation.R

class FaqMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<FaqState.Event>()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen() {
        composeRule.setContent {
            ScreenPreviewTheme {
                FaqMobileScreen(
                    state = FaqState.State,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleAndFirstQuestion() {
        setScreen()

        composeRule.onNodeWithText(string(R.string.faq_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.faq_who_am_i_title)).assertIsDisplayed()
    }

    @Test
    fun questionExpandsAndCollapsesOnClick() {
        setScreen()
        val question = composeRule.onNodeWithText(string(R.string.faq_who_am_i_title))
        val answer = string(R.string.faq_who_am_i_answer)

        composeRule.onNodeWithText(answer).assertDoesNotExist()

        question.performClick()
        composeRule.onNodeWithText(answer).assertIsDisplayed()

        question.performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText(answer).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen()

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertEquals(listOf<FaqState.Event>(FaqState.Event.BackSelected), events)
    }
}
