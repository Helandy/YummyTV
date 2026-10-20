package su.afk.yummy.tv.feature.account.mobile.usersearch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.account.model.UserSearchItem
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.usersearch.UserSearchState

class UserSearchMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<UserSearchState.Event>()

    private val loaded = LoadStates(
        refresh = LoadState.NotLoading(endOfPaginationReached = false),
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = LoadState.NotLoading(endOfPaginationReached = true),
    )

    private val user = UserSearchItem(id = 5, nickname = "ТестовыйЮзер", avatarUrl = null, lastOnlineSeconds = 0L)

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: UserSearchState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                UserSearchMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    private fun active(vararg items: UserSearchItem) = UserSearchState.State(
        query = "тест",
        isSearchActive = true,
        results = flowOf(PagingData.from(items.toList(), sourceLoadStates = loaded)),
    )

    @Test
    fun inactiveSearchShowsHint() {
        setScreen(UserSearchState.State())

        composeRule.onNodeWithText(string(R.string.account_user_search)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.user_search_initial)).assertIsDisplayed()
    }

    @Test
    fun typingSendsQueryChanged() {
        setScreen(UserSearchState.State())

        composeRule.onNode(hasSetTextAction()).performTextInput("ник")

        assertTrue(UserSearchState.Event.QueryChanged("ник") in events)
    }

    @Test
    fun emptyResultsShowMessage() {
        setScreen(active())

        composeRule.onNodeWithText(string(R.string.user_search_empty)).assertIsDisplayed()
    }

    @Test
    fun userIsShownAndClickSendsUserSelected() {
        setScreen(active(user))

        composeRule.onNodeWithText("ТестовыйЮзер").assertIsDisplayed()
        composeRule.onNodeWithText("ТестовыйЮзер").performClick()

        assertTrue(UserSearchState.Event.UserSelected("ТестовыйЮзер", 5) in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(UserSearchState.State())

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(UserSearchState.Event.BackSelected in events)
    }
}
