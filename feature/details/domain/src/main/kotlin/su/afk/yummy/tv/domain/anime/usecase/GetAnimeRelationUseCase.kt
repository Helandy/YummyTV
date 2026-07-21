package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.domain.anime.model.AnimeRelationReference
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

/** Находит связанное аниме по переданной ссылке на отношение. */
class GetAnimeRelationUseCase @Inject constructor(
    private val repository: AnimeRepository,
) {
    suspend operator fun invoke(reference: AnimeRelationReference) =
        repository.getAnimeRelation(reference)
}
