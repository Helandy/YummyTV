package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.mutation.AccountMutationAction
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository

/** Deletes the current user's rating for an anime. */
class DeleteAnimeRatingUseCase(
    private val repository: AnimeExtrasRepository,
    private val mutationErrorNotifier: AccountMutationErrorNotifier,
) {
    suspend operator fun invoke(animeId: Int) =
        notifyMutationFailure(mutationErrorNotifier, AccountMutationAction.DELETE_RATING) {
            repository.deleteRating(animeId)
        }
}
