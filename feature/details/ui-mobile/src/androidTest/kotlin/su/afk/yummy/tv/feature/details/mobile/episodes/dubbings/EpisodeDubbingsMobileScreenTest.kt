package su.afk.yummy.tv.feature.details.mobile.episodes.dubbings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.feature.details.episodes.dubbings.EpisodeDubbingsState
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR

class EpisodeDubbingsMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<EpisodeDubbingsState.Event>()

    private val dubbing = EpisodeDubbingsState.DubbingItem(
        name = "AniLibria",
        views = 1000,
        episodeCount = 12,
        supportedBalancers = "Kodik",
    )

    private fun string(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun setScreen(state: EpisodeDubbingsState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                EpisodeDubbingsMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsEpisodeTitleAndEmptyMessage() {
        setScreen(EpisodeDubbingsState.State(episode = "5", isLoading = false))

        composeRule.onNodeWithText(string(R.string.details_episode_number, "5")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.details_subscriptions_empty)).assertIsDisplayed()
    }

    @Test
    fun dubbingClickSendsDubbingSelected() {
        setScreen(
            EpisodeDubbingsState.State(
                episode = "5",
                isLoading = false,
                dubbings = persistentListOf(dubbing),
            ),
        )

        composeRule.onNodeWithText("AniLibria").performClick()

        assertEquals(listOf<EpisodeDubbingsState.Event>(EpisodeDubbingsState.Event.DubbingSelected("AniLibria")), events)
    }

    @Test
    fun errorRetrySendsRetrySelected() {
        setScreen(
            EpisodeDubbingsState.State(episode = "5", isLoading = false, error = UiText.Res(R.string.details_load_error)),
        )

        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertEquals(listOf<EpisodeDubbingsState.Event>(EpisodeDubbingsState.Event.RetrySelected), events)
    }
}
