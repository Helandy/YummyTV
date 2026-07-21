package su.afk.yummy.tv.domain.messages.usecase

import su.afk.yummy.tv.domain.messages.repository.MessagesRepository
import javax.inject.Inject

/** Удаляет выбранное сообщение из диалога. */
class DeleteMessageUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(messageId: Int) = repository.deleteMessage(messageId)
}
