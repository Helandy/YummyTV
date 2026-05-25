package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.*
import su.afk.yummy.tv.domain.account.repository.*

/** Adds or moves an anime into the selected user list. */
class SetAnimeListUseCase(private val repository: UserListsRepository) {
    suspend operator fun invoke(animeId: Int, list: UserAnimeList) = repository.setAnimeList(animeId, list)
}
