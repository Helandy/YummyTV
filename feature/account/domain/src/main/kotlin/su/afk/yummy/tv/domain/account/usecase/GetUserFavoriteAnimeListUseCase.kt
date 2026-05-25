package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.repository.UserListsRepository

/** Loads the current user's favorite anime list. */
class GetUserFavoriteAnimeListUseCase(private val repository: UserListsRepository) {
    suspend operator fun invoke(userId: Int): List<UserAnimeListItem> =
        repository.getUserFavorites(userId)
}
