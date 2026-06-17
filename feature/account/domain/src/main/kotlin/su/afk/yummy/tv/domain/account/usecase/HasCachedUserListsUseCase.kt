package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.UserListsRepository
import javax.inject.Inject

/** Проверяет, есть ли на устройстве кешированные страницы списков пользователя. */
class HasCachedUserListsUseCase @Inject constructor(
    private val repository: UserListsRepository,
) {
    suspend operator fun invoke(userId: Int): Boolean =
        repository.hasCachedUserLists(userId)
}
