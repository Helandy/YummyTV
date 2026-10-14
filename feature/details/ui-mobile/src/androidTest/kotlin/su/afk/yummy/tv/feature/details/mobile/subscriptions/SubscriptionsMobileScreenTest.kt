package su.afk.yummy.tv.feature.details.mobile.subscriptions

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
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.details.model.SubscriptionOption
import su.afk.yummy.tv.feature.details.subscriptions.SubscriptionsState

class SubscriptionsMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<SubscriptionsState.Event>()

    private val option = SubscriptionOption(
        key = "kodik:anidub",
        playerId = 1,
        player = "Kodik",
        dubbing = "AniDub",
        episodesCount = 12,
        subscriptionVideoId = 100,
        isSubscribed = false,
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: SubscriptionsState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                SubscriptionsMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleAndEmptyMessage() {
        setScreen(SubscriptionsState.State(isLoading = false))

        composeRule.onNodeWithText(string(R.string.details_subscriptions)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.details_subscriptions_empty)).assertIsDisplayed()
    }

    @Test
    fun optionClickSendsSubscriptionToggled() {
        setScreen(SubscriptionsState.State(isLoading = false, subscriptions = persistentListOf(option)))

        composeRule.onNodeWithText("AniDub").performClick()

        assertEquals(listOf<SubscriptionsState.Event>(SubscriptionsState.Event.SubscriptionToggled("kodik:anidub")), events)
    }

    @Test
    fun errorRetrySendsRetrySelected() {
        setScreen(SubscriptionsState.State(isLoading = false, error = UiText.Res(R.string.details_load_error)))

        composeRule.onNodeWithText(string(R.string.details_load_error)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertEquals(listOf<SubscriptionsState.Event>(SubscriptionsState.Event.RetrySelected), events)
    }
}
