package su.afk.yummy.tv.feature.messages.chat.handler

import su.afk.yummy.tv.domain.messages.usecase.ClaimMessageUseCase
import su.afk.yummy.tv.domain.messages.usecase.DeleteMessageUseCase
import su.afk.yummy.tv.domain.messages.usecase.EditMessageUseCase
import su.afk.yummy.tv.domain.messages.usecase.GetMessageHistoryUseCase
import su.afk.yummy.tv.domain.messages.usecase.MarkMessagesReadUseCase
import su.afk.yummy.tv.domain.messages.usecase.RestoreMessageUseCase
import su.afk.yummy.tv.domain.messages.usecase.SendMessageUseCase
import su.afk.yummy.tv.domain.messages.usecase.SetDialogUserBannedUseCase
import javax.inject.Inject

class ChatMutationHandler @Inject constructor(
    private val sendMessage: SendMessageUseCase,
    private val markMessagesRead: MarkMessagesReadUseCase,
    private val editMessage: EditMessageUseCase,
    private val deleteMessage: DeleteMessageUseCase,
    private val restoreMessage: RestoreMessageUseCase,
    private val getMessageHistory: GetMessageHistoryUseCase,
    private val claimMessage: ClaimMessageUseCase,
    private val setDialogUserBanned: SetDialogUserBannedUseCase,
) {
    suspend fun send(userId: Int, text: String) = sendMessage(userId, text)
    suspend fun markRead(userId: Int) = markMessagesRead(userId)
    suspend fun edit(messageId: Int, text: String) = editMessage(messageId, text)
    suspend fun delete(messageId: Int) = deleteMessage(messageId)
    suspend fun restore(messageId: Int) = restoreMessage(messageId)
    suspend fun history(messageId: Int) = getMessageHistory(messageId)
    suspend fun claim(messageId: Int) = claimMessage(messageId)
    suspend fun setBanned(userId: Int, banned: Boolean) = setDialogUserBanned(userId, banned)
}
