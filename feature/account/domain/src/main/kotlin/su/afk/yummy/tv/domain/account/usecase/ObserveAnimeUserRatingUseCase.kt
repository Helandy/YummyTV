package su.afk.yummy.tv.domain.account.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import javax.inject.Inject

/**
 * Наблюдает оценку текущего пользователя (1..10) для аниме из локального кэша.
 *
 * Эмитит `null`, пока оценки нет, и обновляется сразу после её выставления или удаления.
 */
class ObserveAnimeUserRatingUseCase @Inject constructor(private val repository: AnimeExtrasRepository) {
    operator fun invoke(animeId: Int): Flow<Int?> = repository.observeUserRating(animeId)
}
