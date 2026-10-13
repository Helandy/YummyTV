package su.afk.yummy.tv.feature.messages.chat.handler

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.messages.model.ChatMessage
import su.afk.yummy.tv.domain.messages.usecase.GetMessagesUseCase

class ChatPollingHandlerTest : BaseUnitTest() {

    private val getMessages: GetMessagesUseCase = mockk()

    @Test
    fun `polling skips failed requests and keeps going`() = runTest {
        val first = listOf(mockk<ChatMessage>())
        val second = listOf(mockk<ChatMessage>(), mockk<ChatMessage>())
        coEvery { getMessages(USER_ID, 30) } returnsMany listOf(first) andThenThrows
            IllegalStateException("network") andThen second

        val updates = ChatPollingHandler(getMessages).updates(USER_ID).take(2).toList()

        assertEquals(listOf(first, second), updates)
    }

    private companion object {
        const val USER_ID = 5
    }
}
