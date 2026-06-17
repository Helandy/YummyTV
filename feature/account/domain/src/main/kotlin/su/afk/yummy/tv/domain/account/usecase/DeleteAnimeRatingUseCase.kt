package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.mutation.AccountMutationAction
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import javax.inject.Inject

/** Удаляет оценку текущего пользователя для выбранного аниме. */
class DeleteAnimeRatingUseCase @Inject constructor(
    private val repository: AnimeExtrasRepository,
    private val mutationErrorNotifier: AccountMutationErrorNotifier,
) {
    suspend operator fun invoke(animeId: Int) =
        notifyMutationFailure(mutationErrorNotifier, AccountMutationAction.DELETE_RATING) {
            repository.deleteRating(animeId)
        }
}
