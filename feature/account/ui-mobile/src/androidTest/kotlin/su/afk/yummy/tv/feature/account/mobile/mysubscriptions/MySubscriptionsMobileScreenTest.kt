package su.afk.yummy.tv.feature.account.mobile.mysubscriptions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.account.model.VideoSubscription
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.mysubscriptions.MySubscriptionsState
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR

class MySubscriptionsMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<MySubscriptionsState.Event>()

    private val subscription = VideoSubscription(
        animeId = 15,
        animeUrl = "",
        playerId = 1,
        player = "Kodik",
        dubbing = "AniDub",
        poster = null,
        title = "Тестовое аниме",
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: MySubscriptionsState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                MySubscriptionsMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun screenShownEventIsSentOnOpen() {
        setScreen(MySubscriptionsState.State(isLoading = false))
        composeRule.waitForIdle()

        assertTrue(MySubscriptionsState.Event.ScreenShown in events)
    }

    @Test
    fun emptyListShowsMessage() {
        setScreen(MySubscriptionsState.State(isLoading = false))

        composeRule.onNodeWithText(string(R.string.account_my_subscriptions)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.account_my_subscriptions_empty)).assertIsDisplayed()
    }

    @Test
    fun subscriptionClickSendsSubscriptionSelected() {
        setScreen(MySubscriptionsState.State(isLoading = false, subscriptions = persistentListOf(subscription)))

        composeRule.onNodeWithText("Тестовое аниме").performClick()

        assertTrue(MySubscriptionsState.Event.SubscriptionSelected(15) in events)
    }

    @Test
    fun errorShowsMessageAndRetrySendsRetrySelected() {
        setScreen(MySubscriptionsState.State(isLoading = false, error = AccountUiError.LOGOUT_FAILED))

        composeRule.onNodeWithText(string(R.string.account_error_logout_failed)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertTrue(MySubscriptionsState.Event.RetrySelected in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(MySubscriptionsState.State(isLoading = false))
        composeRule.waitForIdle()
        events.clear()

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertEquals(listOf<MySubscriptionsState.Event>(MySubscriptionsState.Event.BackSelected), events)
    }
}
