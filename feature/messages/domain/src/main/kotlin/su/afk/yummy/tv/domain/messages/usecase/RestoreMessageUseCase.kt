package su.afk.yummy.tv.domain.messages.usecase

import su.afk.yummy.tv.domain.messages.repository.MessagesRepository
import javax.inject.Inject

/** Восстанавливает ранее удалённое сообщение. */
class RestoreMessageUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(messageId: Int) = repository.restoreMessage(messageId)
}
