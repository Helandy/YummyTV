package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.repository.UserListsRepository
import javax.inject.Inject

/** Loads a user's anime list for the selected Yani list category. */
class GetUserAnimeListUseCase @Inject constructor(private val repository: UserListsRepository) {
    suspend operator fun invoke(userId: Int, list: UserAnimeList): List<UserAnimeListItem> =
        repository.getUserList(userId, list)
}
