package su.afk.yummy.tv.domain.messages.usecase

import su.afk.yummy.tv.domain.messages.repository.MessagesRepository
import javax.inject.Inject

/** Изменяет текст ранее отправленного сообщения. */
class EditMessageUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(messageId: Int, text: String) =
        repository.editMessage(messageId, text)
}
