package su.afk.yummy.tv.feature.details.mobile.rating

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
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.rating.RatingState
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR

class RatingMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<RatingState.Event>()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: RatingState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                RatingMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleAndRatingPicker() {
        setScreen(RatingState.State(isLoading = false))

        composeRule.onNodeWithText(string(R.string.details_rating_button)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.details_mobile_rating_pick)).assertIsDisplayed()
    }

    @Test
    fun loadingHidesRatingPicker() {
        setScreen(RatingState.State(isLoading = true))

        composeRule.onNodeWithText(string(R.string.details_mobile_rating_pick)).assertDoesNotExist()
    }

    @Test
    fun deleteButtonIsHiddenWithoutUserRating() {
        setScreen(RatingState.State(isLoading = false))

        composeRule.onNodeWithText(string(R.string.details_rating_delete)).assertDoesNotExist()
    }

    @Test
    fun deleteButtonSendsRatingDeleted() {
        setScreen(RatingState.State(isLoading = false, ratingSummary = AnimeRatingSummary(userRating = 5)))

        composeRule.onNodeWithText(string(R.string.details_rating_delete)).performClick()

        assertEquals(listOf<RatingState.Event>(RatingState.Event.RatingDeleted), events)
    }

    @Test
    fun ratingButtonSendsRatingSelected() {
        setScreen(RatingState.State(isLoading = false))

        composeRule.onNodeWithText("7").performClick()

        assertTrue(RatingState.Event.RatingSelected(7) in events)
    }

    @Test
    fun errorRetrySendsRetrySelected() {
        setScreen(RatingState.State(isLoading = false, error = UiText.Res(R.string.details_load_error)))

        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertEquals(listOf<RatingState.Event>(RatingState.Event.RetrySelected), events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(RatingState.State(isLoading = false))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertEquals(listOf<RatingState.Event>(RatingState.Event.BackSelected), events)
    }
}
