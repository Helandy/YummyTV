package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.utils.notifyMutationFailure
import su.afk.yummy.tv.domain.account.model.AccountMutationAction
import su.afk.yummy.tv.domain.account.repository.AccountMutationErrorRepository
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import javax.inject.Inject

/** Удаляет оценку текущего пользователя для выбранного аниме. */
class DeleteAnimeRatingUseCase @Inject constructor(
    private val repository: AnimeExtrasRepository,
    private val mutationErrorNotifier: AccountMutationErrorRepository,
) {
    suspend operator fun invoke(animeId: Int) =
        notifyMutationFailure(mutationErrorNotifier, AccountMutationAction.DELETE_RATING) {
            repository.deleteRating(animeId)
        }
}
