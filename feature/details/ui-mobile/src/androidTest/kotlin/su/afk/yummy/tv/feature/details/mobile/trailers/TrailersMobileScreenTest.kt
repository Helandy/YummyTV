package su.afk.yummy.tv.feature.details.mobile.trailers

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
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import su.afk.yummy.tv.feature.details.trailers.TrailersState

class TrailersMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<TrailersState.Event>()

    private fun string(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun setScreen(state: TrailersState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                TrailersMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleAndEmptyMessage() {
        setScreen(TrailersState.State(isLoading = false))

        composeRule.onNodeWithText(string(R.string.details_trailers)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.empty_screen)).assertIsDisplayed()
    }

    @Test
    fun loadingHidesEmptyMessage() {
        setScreen(TrailersState.State(isLoading = true))

        composeRule.onNodeWithText(string(DesignSystemR.string.empty_screen)).assertDoesNotExist()
    }

    @Test
    fun trailersAreNumbered() {
        setScreen(
            TrailersState.State(
                isLoading = false,
                trailers = persistentListOf(
                    AnimeTrailer("https://www.youtube.com/embed/aaaaaaaaaaa"),
                    AnimeTrailer("https://www.youtube.com/embed/bbbbbbbbbbb"),
                ),
            ),
        )

        composeRule.onNodeWithText(string(R.string.details_trailer_number, 1)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.details_trailer_number, 2)).assertIsDisplayed()
    }

    @Test
    fun errorRetrySendsRetrySelected() {
        setScreen(TrailersState.State(isLoading = false, error = UiText.Res(R.string.details_load_error)))

        composeRule.onNodeWithText(string(R.string.details_load_error)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertEquals(listOf<TrailersState.Event>(TrailersState.Event.RetrySelected), events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(TrailersState.State(isLoading = false))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertEquals(listOf<TrailersState.Event>(TrailersState.Event.BackSelected), events)
    }
}
