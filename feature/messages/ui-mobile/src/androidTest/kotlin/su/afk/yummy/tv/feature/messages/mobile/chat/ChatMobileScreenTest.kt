package su.afk.yummy.tv.feature.messages.mobile.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.messages.model.ChatMessage
import su.afk.yummy.tv.domain.messages.model.DialogSummary
import su.afk.yummy.tv.feature.messages.chat.ChatState
import su.afk.yummy.tv.feature.messages.presentation.R

class ChatMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<ChatState.Event>()

    private val peer = DialogSummary(
        userId = 15,
        nickname = "Собеседник",
        avatarUrl = null,
        roles = emptyList(),
        isBanned = false,
        lastMessage = "",
        unreadCount = 0,
        dateSeconds = 0L,
        lastOnlineSeconds = 0L,
    )

    private val message = ChatMessage(
        id = 100,
        text = "Привет, как дела?",
        dateSeconds = 0L,
        fromUserId = 15,
        toUserId = 1,
        nickname = "Собеседник",
        avatarUrl = null,
        roles = emptyList(),
        isRead = true,
        isDeleted = false,
        deletedByUserId = null,
        isEdited = false,
        editedByUserId = null,
        reply = null,
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun authorized(
        messages: List<ChatMessage> = emptyList(),
        block: ChatState.State.() -> ChatState.State = { this },
    ) = ChatState.State(
        userId = 15,
        currentUserId = 1,
        isAuthResolved = true,
        isAuthorized = true,
        peer = peer,
        messages = persistentListOf<ChatMessage>().addAll(messages),
    ).block()

    private fun setScreen(state: ChatState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                ChatMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun screenStartedIsSentOnOpen() {
        setScreen(authorized())
        composeRule.waitForIdle()

        assertTrue(ChatState.Event.ScreenStarted in events)
    }

    @Test
    fun unauthorizedShowsSignIn() {
        setScreen(ChatState.State(userId = 15, isAuthResolved = true, isAuthorized = false))

        composeRule.onNodeWithText(string(R.string.messages_auth_required)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_sign_in)).performClick()

        assertTrue(ChatState.Event.LoginSelected in events)
    }

    @Test
    fun peerNicknameIsShownInTitle() {
        setScreen(authorized())

        composeRule.onNodeWithText("Собеседник").assertIsDisplayed()
    }

    @Test
    fun globalChatTitleIsShownForUserIdZero() {
        setScreen(ChatState.State(userId = 0, isAuthResolved = true, isAuthorized = true))

        composeRule.onNodeWithText(string(R.string.messages_global_chat)).assertIsDisplayed()
    }

    @Test
    fun emptyChatShowsMessage() {
        setScreen(authorized())

        composeRule.onNodeWithText(string(R.string.messages_chat_empty)).assertIsDisplayed()
    }

    @Test
    fun loadErrorRetrySendsRefreshSelected() {
        setScreen(authorized { copy(hasLoadError = true) })

        composeRule.onNodeWithText(string(R.string.messages_chat_error)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertTrue(ChatState.Event.RefreshSelected in events)
    }

    @Test
    fun messageTextIsShown() {
        setScreen(authorized(listOf(message)))

        composeRule.onNodeWithText("Привет, как дела?").assertIsDisplayed()
    }

    @Test
    fun typingSendsDraftChanged() {
        setScreen(authorized())

        composeRule.onNode(hasSetTextAction()).performTextInput("Текст")

        assertTrue(ChatState.Event.DraftChanged("Текст") in events)
    }

    @Test
    fun sendButtonSendsSendSelected() {
        setScreen(authorized { copy(draft = "Текст") })

        composeRule.onNodeWithContentDescription(string(R.string.messages_send)).performClick()

        assertTrue(ChatState.Event.SendSelected in events)
    }

    @Test
    fun bannedPeerShowsNotice() {
        setScreen(authorized { copy(peer = this@ChatMobileScreenTest.peer.copy(isBanned = true)) })

        composeRule.onNodeWithText(string(R.string.messages_banned_notice)).assertIsDisplayed()
    }

    @Test
    fun deleteDialogConfirmSendsDeleteConfirmed() {
        setScreen(authorized(listOf(message)) { copy(pendingDeleteMessageId = 100) })

        composeRule.onNodeWithText(string(R.string.messages_delete_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_delete)).performClick()

        assertTrue(ChatState.Event.DeleteConfirmed in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(authorized())

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(ChatState.Event.BackSelected in events)
    }
}
