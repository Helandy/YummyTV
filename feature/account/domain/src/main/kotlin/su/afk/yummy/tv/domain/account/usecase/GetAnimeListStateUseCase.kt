package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.repository.UserListsRepository

/** Loads the current user's list state for a single anime. */
class GetAnimeListStateUseCase(private val repository: UserListsRepository) {
    suspend operator fun invoke(animeId: Int): UserAnimeListItem? = repository.getAnimeListState(animeId)
}
