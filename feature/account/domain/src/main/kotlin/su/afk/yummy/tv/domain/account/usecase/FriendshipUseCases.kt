package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.UserDirectoryRepository
import javax.inject.Inject

class GetFriendshipUseCase @Inject constructor(private val repository: UserDirectoryRepository) {
    suspend operator fun invoke(userId: Int, friendId: Int) =
        repository.getFriendship(userId, friendId)
}

class AddFriendUseCase @Inject constructor(private val repository: UserDirectoryRepository) {
    suspend operator fun invoke(userId: Int, friendId: Int) = repository.addFriend(userId, friendId)
}

class RemoveFriendUseCase @Inject constructor(private val repository: UserDirectoryRepository) {
    suspend operator fun invoke(userId: Int, friendId: Int) =
        repository.removeFriend(userId, friendId)
}
