package su.afk.yummy.tv.domain.messages.usecase

import su.afk.yummy.tv.domain.messages.repository.MessagesRepository
import javax.inject.Inject

/** Загружает страницу сообщений диалога с выбранным пользователем. */
class GetMessagesUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(userId: Int, limit: Int, startFrom: Int = 0) =
        repository.messages(userId, limit, startFrom)
}
