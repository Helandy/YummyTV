package su.afk.yummy.tv.domain.messages.usecase

import su.afk.yummy.tv.domain.messages.repository.MessagesRepository
import javax.inject.Inject

/** Загружает историю изменений выбранного сообщения. */
class GetMessageHistoryUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(messageId: Int) = repository.messageHistory(messageId)
}
