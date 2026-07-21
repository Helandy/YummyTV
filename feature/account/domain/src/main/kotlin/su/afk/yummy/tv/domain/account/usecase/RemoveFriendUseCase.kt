package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.UserDirectoryRepository
import javax.inject.Inject

/** Удаляет выбранного пользователя из друзей. */
class RemoveFriendUseCase @Inject constructor(private val repository: UserDirectoryRepository) {
    suspend operator fun invoke(userId: Int, friendId: Int) =
        repository.removeFriend(userId, friendId)
}
