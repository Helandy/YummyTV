package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.UserDirectoryRepository
import javax.inject.Inject

/** Добавляет выбранного пользователя в друзья. */
class AddFriendUseCase @Inject constructor(private val repository: UserDirectoryRepository) {
    suspend operator fun invoke(userId: Int, friendId: Int) = repository.addFriend(userId, friendId)
}
