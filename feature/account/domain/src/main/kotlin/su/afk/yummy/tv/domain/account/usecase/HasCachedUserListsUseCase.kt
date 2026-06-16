package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.UserListsRepository
import javax.inject.Inject

/** Returns whether this user already has cached remote list pages on the device. */
class HasCachedUserListsUseCase @Inject constructor(
    private val repository: UserListsRepository,
) {
    suspend operator fun invoke(userId: Int): Boolean =
        repository.hasCachedUserLists(userId)
}
