package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.repository.UserListsRepository
import javax.inject.Inject

/** Загружает состояние выбранного аниме в списках текущего пользователя. */
class GetAnimeListStateUseCase @Inject constructor(private val repository: UserListsRepository) {
    suspend operator fun invoke(animeId: Int): UserAnimeListItem? = repository.getAnimeListState(animeId)
}
