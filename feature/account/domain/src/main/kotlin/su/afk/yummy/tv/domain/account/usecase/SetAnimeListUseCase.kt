package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.mutation.AccountMutationAction
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.repository.UserListsRepository

/** Adds or moves an anime into the selected user list. */
class SetAnimeListUseCase(
    private val repository: UserListsRepository,
    private val mutationErrorNotifier: AccountMutationErrorNotifier,
) {
    suspend operator fun invoke(animeId: Int, list: UserAnimeList) =
        notifyMutationFailure(mutationErrorNotifier, AccountMutationAction.SET_ANIME_LIST) {
            repository.setAnimeList(animeId, list)
        }
}
