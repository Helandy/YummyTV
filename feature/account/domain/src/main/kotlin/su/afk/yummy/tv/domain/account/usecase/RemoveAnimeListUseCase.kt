package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.*
import su.afk.yummy.tv.domain.account.repository.*

/** Removes an anime from the current user's remote list. */
class RemoveAnimeListUseCase(private val repository: UserListsRepository) {
    suspend operator fun invoke(animeId: Int) = repository.removeAnimeList(animeId)
}
