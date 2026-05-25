package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.*
import su.afk.yummy.tv.domain.account.repository.*

/** Updates the favorite flag for an anime in the current user's list. */
class SetAnimeFavoriteUseCase(private val repository: UserListsRepository) {
    suspend operator fun invoke(animeId: Int, favorite: Boolean) = repository.setFavorite(animeId, favorite)
}
