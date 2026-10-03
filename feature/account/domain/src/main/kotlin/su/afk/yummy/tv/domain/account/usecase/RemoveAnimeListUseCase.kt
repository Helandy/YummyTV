package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.utils.notifyMutationFailure
import su.afk.yummy.tv.domain.account.model.AccountMutationAction
import su.afk.yummy.tv.domain.account.repository.AccountMutationErrorRepository
import su.afk.yummy.tv.domain.account.repository.UserListsRepository
import javax.inject.Inject

/** Удаляет аниме из удалённого списка текущего пользователя. */
class RemoveAnimeListUseCase @Inject constructor(
    private val repository: UserListsRepository,
    private val mutationErrorNotifier: AccountMutationErrorRepository,
) {
    suspend operator fun invoke(animeId: Int) =
        notifyMutationFailure(mutationErrorNotifier, AccountMutationAction.REMOVE_ANIME_LIST) {
            repository.removeAnimeList(animeId)
        }
}
