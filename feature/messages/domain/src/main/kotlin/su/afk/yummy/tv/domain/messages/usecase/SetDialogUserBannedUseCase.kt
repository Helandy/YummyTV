package su.afk.yummy.tv.domain.messages.usecase

import su.afk.yummy.tv.domain.messages.repository.MessagesRepository
import javax.inject.Inject

/** Блокирует или разблокирует пользователя в диалоге. */
class SetDialogUserBannedUseCase @Inject constructor(private val repository: MessagesRepository) {
    suspend operator fun invoke(userId: Int, banned: Boolean) =
        repository.setUserBanned(userId, banned)
}
