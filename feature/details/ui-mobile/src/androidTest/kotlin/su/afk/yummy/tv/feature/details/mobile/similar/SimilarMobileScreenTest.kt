package su.afk.yummy.tv.feature.details.mobile.similar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.details.model.SimilarUiState
import su.afk.yummy.tv.feature.details.similar.SimilarState

class SimilarMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<SimilarState.Event>()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: SimilarState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                SimilarMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleAndSourceTabs() {
        setScreen(SimilarState.State(similarState = SimilarUiState.Empty))

        composeRule.onNodeWithText(string(R.string.details_similar)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.details_mobile_similar_source_ai)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.details_mobile_similar_source_site)).assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsMessage() {
        setScreen(SimilarState.State(similarState = SimilarUiState.Empty))

        composeRule.onNodeWithText(string(R.string.details_mobile_similar_empty)).assertIsDisplayed()
    }

    @Test
    fun errorRetrySendsRetrySelected() {
        setScreen(
            SimilarState.State(similarState = SimilarUiState.Error(UiText.Res(R.string.details_load_error))),
        )

        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertEquals(listOf<SimilarState.Event>(SimilarState.Event.RetrySelected), events)
    }

    @Test
    fun ignoreButtonSendsVisibilityToggled() {
        setScreen(SimilarState.State(similarState = SimilarUiState.Empty))

        composeRule.onNodeWithText(string(R.string.details_ignore_recommendation)).performClick()

        assertEquals(listOf<SimilarState.Event>(SimilarState.Event.RecommendationVisibilityToggled), events)
    }

    @Test
    fun ignoredStateShowsRestoreButton() {
        setScreen(SimilarState.State(similarState = SimilarUiState.Empty, isRecommendationIgnored = true))

        composeRule.onNodeWithText(string(R.string.details_restore_recommendation)).assertIsDisplayed()
    }

    @Test
    fun aiTabSendsSourceSelected() {
        setScreen(SimilarState.State(similarState = SimilarUiState.Empty))

        composeRule.onNodeWithText(string(R.string.details_mobile_similar_source_ai)).performClick()
        composeRule.waitForIdle()

        assertTrue(SimilarState.Event.SourceSelected(true) in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(SimilarState.State(similarState = SimilarUiState.Empty))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(SimilarState.Event.BackSelected in events)
    }
}
