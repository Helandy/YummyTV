package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.utils.notifyMutationFailure
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.AccountMutationAction
import su.afk.yummy.tv.domain.account.repository.AccountMutationErrorRepository
import su.afk.yummy.tv.domain.account.repository.UserListsRepository
import javax.inject.Inject

/** Добавляет или переносит аниме в выбранный пользовательский список. */
class SetAnimeListUseCase @Inject constructor(
    private val repository: UserListsRepository,
    private val mutationErrorNotifier: AccountMutationErrorRepository,
) {
    suspend operator fun invoke(animeId: Int, list: UserAnimeList) =
        notifyMutationFailure(mutationErrorNotifier, AccountMutationAction.SET_ANIME_LIST) {
            repository.setAnimeList(animeId, list)
        }
}
