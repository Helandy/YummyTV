package su.afk.yummy.tv.source.anilibria.domain.usecase

import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaRelease
import su.afk.yummy.tv.source.anilibria.domain.repository.AniLibriaRepository
import javax.inject.Inject

/**
 * Находит релизы AniLibria для тайтла по id MyAnimeList, перебирая названия тайтла (озвучка и новый дубляж — разные
 * релизы); если у релиза MAL id не заполнен, сопоставляет его по точному названию и году.
 */
class FindAniLibriaReleasesUseCase @Inject constructor(
    private val repository: AniLibriaRepository,
) {
    suspend operator fun invoke(malId: Int, titles: List<String>, year: Int?): List<AniLibriaRelease> =
        repository.findReleases(malId, titles, year)
}
