package su.afk.yummy.tv.source.anilibria.domain.usecase

import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaRelease
import su.afk.yummy.tv.source.anilibria.domain.repository.AniLibriaRepository
import javax.inject.Inject

/**
 * Находит релиз AniLibria для тайтла по id MyAnimeList, перебирая названия тайтла; если у релиза MAL id не заполнен,
 * сопоставляет его по точному названию и году.
 */
class FindAniLibriaReleaseUseCase @Inject constructor(
    private val repository: AniLibriaRepository,
) {
    suspend operator fun invoke(malId: Int, titles: List<String>, year: Int?): AniLibriaRelease? =
        repository.findRelease(malId, titles, year)
}
