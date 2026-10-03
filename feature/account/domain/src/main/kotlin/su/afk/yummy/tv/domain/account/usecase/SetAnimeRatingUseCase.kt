package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.utils.notifyMutationFailure
import su.afk.yummy.tv.domain.account.model.AccountMutationAction
import su.afk.yummy.tv.domain.account.repository.AccountMutationErrorRepository
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import javax.inject.Inject

/** Сохраняет оценку текущего пользователя для выбранного аниме. */
class SetAnimeRatingUseCase @Inject constructor(
    private val repository: AnimeExtrasRepository,
    private val mutationErrorNotifier: AccountMutationErrorRepository,
) {
    suspend operator fun invoke(animeId: Int, rating: Int) =
        notifyMutationFailure(mutationErrorNotifier, AccountMutationAction.SET_RATING) {
            repository.setRating(animeId, rating)
        }
}
