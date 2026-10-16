package su.afk.yummy.tv.feature.home.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.feature.home.HomeState
import su.afk.yummy.tv.feature.home.model.HomeAnnouncement
import su.afk.yummy.tv.feature.home.presentation.R

class HomeMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<HomeState.Event>()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun loaded(block: HomeState.State.() -> HomeState.State = { this }) =
        HomeState.State(
            isLoading = false,
            isContinueWatchingLoaded = true,
            feed = HomeFeed(heroItems = emptyList(), sections = emptyList()),
        ).block()

    private fun setScreen(state: HomeState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                HomeMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    private fun scrollFeedToText(text: String) {
        composeRule.onNode(hasTestTag("home_feed") and hasScrollAction())
            .performScrollToNode(hasText(text))
    }

    @Test
    fun screenResumedIsSentOnOpen() {
        setScreen(loaded())
        composeRule.waitForIdle()

        assertTrue(HomeState.Event.ScreenResumed in events)
    }

    @Test
    fun reviewsQuickActionSendsReviewsSelected() {
        setScreen(loaded())

        scrollFeedToText(string(R.string.home_mobile_reviews))
        composeRule.onNodeWithText(string(R.string.home_mobile_reviews)).performClick()

        assertTrue(HomeState.Event.ReviewsSelected in events)
    }

    @Test
    fun scheduleQuickActionIsHiddenWithoutSchedule() {
        setScreen(loaded { copy(hasSchedule = false) })

        scrollFeedToText(string(R.string.home_mobile_reviews))
        composeRule.onNodeWithText(string(R.string.home_mobile_schedule)).assertDoesNotExist()
    }

    @Test
    fun scheduleQuickActionSendsScheduleSelected() {
        setScreen(loaded { copy(hasSchedule = true) })

        scrollFeedToText(string(R.string.home_mobile_schedule))
        composeRule.onNodeWithText(string(R.string.home_mobile_schedule)).performClick()

        assertTrue(HomeState.Event.ScheduleSelected in events)
    }

    @Test
    fun errorRetrySendsRetrySelected() {
        setScreen(loaded { copy(error = UiText.Res(R.string.home_load_error)) })

        composeRule.onNodeWithText(string(R.string.home_load_error)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertTrue(HomeState.Event.RetrySelected in events)
    }

    @Test
    fun announcementDialogIsShownAndDismissed() {
        setScreen(
            loaded {
                copy(announcement = HomeAnnouncement(id = "a1", title = "Важное", message = "Текст объявления", buttonText = "Понятно"))
            },
        )

        composeRule.onNodeWithText("Текст объявления").assertIsDisplayed()
        composeRule.onNodeWithText("Понятно").performClick()

        assertTrue(HomeState.Event.AnnouncementDismissed in events)
    }

    @Test
    fun supportPromptIsShownAndDismissed() {
        setScreen(loaded { copy(supportPromptVisible = true) })

        composeRule.onNodeWithText(string(R.string.home_support_prompt_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.home_support_prompt_ok)).performClick()

        assertTrue(HomeState.Event.SupportPromptDismissed in events)
    }
}
