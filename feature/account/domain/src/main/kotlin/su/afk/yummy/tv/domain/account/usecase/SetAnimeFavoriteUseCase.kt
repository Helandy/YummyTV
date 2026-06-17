package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.mutation.AccountMutationAction
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.repository.UserListsRepository
import javax.inject.Inject

/** Обновляет признак избранного для аниме в списках пользователя. */
class SetAnimeFavoriteUseCase @Inject constructor(
    private val repository: UserListsRepository,
    private val mutationErrorNotifier: AccountMutationErrorNotifier,
) {
    suspend operator fun invoke(animeId: Int, favorite: Boolean) =
        notifyMutationFailure(
            mutationErrorNotifier,
            if (favorite) AccountMutationAction.SET_FAVORITE else AccountMutationAction.REMOVE_FAVORITE,
        ) {
            repository.setFavorite(animeId, favorite)
        }
}
