package su.afk.yummy.tv.feature.messages.mobile.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import su.afk.yummy.tv.domain.messages.model.DialogSummary
import su.afk.yummy.tv.feature.messages.dialogs.DialogsState
import su.afk.yummy.tv.feature.messages.presentation.R

class DialogsMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<DialogsState.Event>()

    private val loaded = LoadStates(
        refresh = LoadState.NotLoading(endOfPaginationReached = false),
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = LoadState.NotLoading(endOfPaginationReached = true),
    )

    private val dialog = DialogSummary(
        userId = 15,
        nickname = "Собеседник",
        avatarUrl = null,
        roles = emptyList(),
        isBanned = false,
        lastMessage = "Последнее сообщение",
        unreadCount = 2,
        dateSeconds = 0L,
        lastOnlineSeconds = 0L,
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun authorized(vararg items: DialogSummary) = DialogsState.State(
        isAuthResolved = true,
        isAuthorized = true,
        dialogs = flowOf(PagingData.from(items.toList(), sourceLoadStates = loaded)),
    )

    private fun setScreen(state: DialogsState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                DialogsMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun unauthorizedShowsSignInAndSendsLoginSelected() {
        setScreen(DialogsState.State(isAuthResolved = true, isAuthorized = false))

        composeRule.onNodeWithText(string(R.string.messages_auth_required)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_sign_in)).performClick()

        assertTrue(DialogsState.Event.LoginSelected in events)
    }

    @Test
    fun authorizedWithoutDialogsShowsEmptyMessage() {
        setScreen(authorized())

        composeRule.onNodeWithText(string(R.string.messages_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.messages_empty)).assertIsDisplayed()
    }

    @Test
    fun dialogRowShowsNicknameAndLastMessage() {
        setScreen(authorized(dialog))

        composeRule.onNodeWithText("Собеседник").assertIsDisplayed()
        composeRule.onNodeWithText("Последнее сообщение").assertIsDisplayed()
    }

    @Test
    fun dialogClickSendsDialogSelected() {
        setScreen(authorized(dialog))

        composeRule.onNodeWithText("Собеседник").performClick()

        assertTrue(DialogsState.Event.DialogSelected(15) in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(authorized())

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(DialogsState.Event.BackSelected in events)
    }
}
