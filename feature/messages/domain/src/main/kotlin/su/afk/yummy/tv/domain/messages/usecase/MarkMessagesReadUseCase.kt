package su.afk.yummy.tv.domain.messages.usecase

import su.afk.yummy.tv.domain.messages.repository.MessagesRepository
import javax.inject.Inject

/** Помечает сообщения диалога с выбранным пользователем как прочитанные. */
class MarkMessagesReadUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(userId: Int) = repository.markRead(userId)
}
