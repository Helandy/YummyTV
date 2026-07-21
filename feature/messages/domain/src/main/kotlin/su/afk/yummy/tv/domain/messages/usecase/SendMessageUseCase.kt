package su.afk.yummy.tv.domain.messages.usecase

import su.afk.yummy.tv.domain.messages.repository.MessagesRepository
import javax.inject.Inject

/** Отправляет сообщение выбранному пользователю. */
class SendMessageUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(userId: Int, text: String) = repository.sendMessage(userId, text)
}
