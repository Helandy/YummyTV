package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.repository.UserListsRepository
import javax.inject.Inject

/** Загружает список избранного аниме текущего пользователя. */
class GetUserFavoriteAnimeListUseCase @Inject constructor(private val repository: UserListsRepository) {
    suspend operator fun invoke(
        userId: Int,
        forceRefresh: Boolean = false,
    ): List<UserAnimeListItem> =
        repository.getUserFavorites(userId, forceRefresh)
}
