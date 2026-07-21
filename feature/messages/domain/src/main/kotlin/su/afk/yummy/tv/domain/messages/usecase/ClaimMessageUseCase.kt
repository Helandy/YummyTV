package su.afk.yummy.tv.domain.messages.usecase

import su.afk.yummy.tv.domain.messages.repository.MessagesRepository
import javax.inject.Inject

/** Отправляет жалобу на выбранное сообщение. */
class ClaimMessageUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(messageId: Int) = repository.claimMessage(messageId)
}
