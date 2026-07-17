package su.afk.yummy.tv.domain.messages.repository

import su.afk.yummy.tv.domain.messages.model.ChatMessage
import su.afk.yummy.tv.domain.messages.model.DialogSummary
import su.afk.yummy.tv.domain.messages.model.MessageHistoryEntry

interface MessagesRepository {
    suspend fun dialogs(limit: Int, offset: Int, needUserId: Int? = null): List<DialogSummary>
    suspend fun messages(userId: Int, limit: Int, startFrom: Int = 0): List<ChatMessage>
    suspend fun sendMessage(userId: Int, text: String): ChatMessage
    suspend fun markRead(userId: Int): Boolean
    suspend fun editMessage(messageId: Int, text: String): ChatMessage
    suspend fun deleteMessage(messageId: Int): ChatMessage
    suspend fun restoreMessage(messageId: Int): ChatMessage
    suspend fun messageHistory(messageId: Int): List<MessageHistoryEntry>
    suspend fun claimMessage(messageId: Int): Boolean
    suspend fun setUserBanned(userId: Int, banned: Boolean): Boolean
}
