package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.UserProfileContentRepository
import javax.inject.Inject

/** Загружает страницу друзей выбранного пользователя. */
class GetUserFriendsUseCase @Inject constructor(
    private val repository: UserProfileContentRepository,
) {
    suspend operator fun invoke(userId: Int, limit: Int, offset: Int) =
        repository.getFriends(userId, limit, offset)
}
