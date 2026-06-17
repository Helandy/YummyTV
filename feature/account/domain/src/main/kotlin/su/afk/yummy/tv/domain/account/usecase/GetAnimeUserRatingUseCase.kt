package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import javax.inject.Inject

/** Загружает оценку текущего пользователя для аниме, если она сохранена. */
class GetAnimeUserRatingUseCase @Inject constructor(private val repository: AnimeExtrasRepository) {
    suspend operator fun invoke(animeId: Int): Int? = repository.getUserRating(animeId)
}
