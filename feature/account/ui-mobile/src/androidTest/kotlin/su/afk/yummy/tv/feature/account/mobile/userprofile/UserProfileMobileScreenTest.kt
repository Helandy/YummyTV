package su.afk.yummy.tv.feature.account.mobile.userprofile

import kotlinx.collections.immutable.toPersistentList
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR

class UserProfileMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<UserProfileState.Event>()

    private val profile = UserProfileSummary(userId = 12, nickname = "ТестовыйПрофиль", about = "О себе")

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun loaded(block: UserProfileState.State.() -> UserProfileState.State = { this }) =
        UserProfileState.State(
            userId = 12,
            profile = profile,
            isOverviewLoading = false,
            visibleTabs = UserProfileState.Tab.entries.toPersistentList(),
        ).block()

    private fun setScreen(state: UserProfileState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                UserProfileMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsNicknameInTopBarAndHeader() {
        setScreen(loaded())

        // ник есть и в топ-баре, и в шапке профиля
        composeRule.onAllNodesWithText("ТестовыйПрофиль", substring = true).assertCountEquals(2)
    }

    @Test
    fun overviewErrorRetrySendsRetryOverviewSelected() {
        setScreen(UserProfileState.State(isOverviewLoading = false, overviewError = true))

        composeRule.onNodeWithText(string(R.string.user_profile_load_error)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertTrue(UserProfileState.Event.RetryOverviewSelected in events)
    }

    @Test
    fun postsTabSendsTabSelected() {
        setScreen(loaded())

        composeRule.onNodeWithText(string(R.string.user_profile_tab_posts), substring = true).performClick()
        composeRule.waitForIdle()

        assertTrue(UserProfileState.Event.TabSelected(UserProfileState.Tab.POSTS) in events)
    }

    @Test
    fun foreignProfileMessageButtonSendsMessageSelected() {
        setScreen(loaded { copy(isOwnProfile = false, isAuthorized = true) })

        composeRule.onNodeWithText(string(R.string.user_profile_message)).performClick()

        assertTrue(UserProfileState.Event.MessageSelected in events)
    }

    @Test
    fun authorizedUserCanAddFriend() {
        setScreen(loaded { copy(isAuthorized = true) })

        composeRule.onNodeWithText(string(R.string.user_profile_add_friend)).performClick()

        assertTrue(UserProfileState.Event.FriendshipActionSelected in events)
    }

    @Test
    fun guestIsAskedToSignInToAddFriend() {
        setScreen(loaded { copy(isAuthorized = false) })

        composeRule.onNodeWithText(string(R.string.user_profile_login_to_friend)).performClick()

        assertTrue(UserProfileState.Event.LoginToFriendSelected in events)
    }

    @Test
    fun friendshipErrorIsShown() {
        setScreen(loaded { copy(isAuthorized = true, friendshipError = true) })

        composeRule.onNodeWithText(string(R.string.user_profile_friendship_error)).assertIsDisplayed()
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(loaded())

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(UserProfileState.Event.BackSelected in events)
    }
}
