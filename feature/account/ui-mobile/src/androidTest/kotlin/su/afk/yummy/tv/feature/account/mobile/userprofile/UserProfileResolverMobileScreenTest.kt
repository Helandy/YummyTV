package su.afk.yummy.tv.feature.account.mobile.userprofile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.userprofile.UserProfileResolverState

class UserProfileResolverMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<UserProfileResolverState.Event>()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: UserProfileResolverState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                UserProfileResolverMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun loadingHidesNotFoundMessage() {
        setScreen(UserProfileResolverState.State(isLoading = true))

        composeRule.onNodeWithText(string(R.string.user_profile_not_found)).assertDoesNotExist()
    }

    @Test
    fun notFoundShowsMessageAndRetrySendsRetrySelected() {
        setScreen(UserProfileResolverState.State(isLoading = false, hasError = true))

        composeRule.onNodeWithText(string(R.string.user_profile_not_found)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertTrue(UserProfileResolverState.Event.RetrySelected in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(UserProfileResolverState.State(isLoading = false))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(UserProfileResolverState.Event.BackSelected in events)
    }
}
