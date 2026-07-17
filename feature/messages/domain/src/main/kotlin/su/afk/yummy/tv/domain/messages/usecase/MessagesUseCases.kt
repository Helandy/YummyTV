package su.afk.yummy.tv.domain.messages.usecase

import su.afk.yummy.tv.domain.messages.repository.MessagesRepository
import javax.inject.Inject

class GetDialogsUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(limit: Int, offset: Int, needUserId: Int? = null) =
        repository.dialogs(limit, offset, needUserId)
}

class GetMessagesUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(userId: Int, limit: Int, startFrom: Int = 0) =
        repository.messages(userId, limit, startFrom)
}

class SendMessageUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(userId: Int, text: String) = repository.sendMessage(userId, text)
}

class MarkMessagesReadUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(userId: Int) = repository.markRead(userId)
}

class EditMessageUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(messageId: Int, text: String) =
        repository.editMessage(messageId, text)
}

class DeleteMessageUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(messageId: Int) = repository.deleteMessage(messageId)
}

class RestoreMessageUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(messageId: Int) = repository.restoreMessage(messageId)
}

class GetMessageHistoryUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(messageId: Int) = repository.messageHistory(messageId)
}

class ClaimMessageUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(messageId: Int) = repository.claimMessage(messageId)
}

class SetDialogUserBannedUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(userId: Int, banned: Boolean) =
        repository.setUserBanned(userId, banned)
}
