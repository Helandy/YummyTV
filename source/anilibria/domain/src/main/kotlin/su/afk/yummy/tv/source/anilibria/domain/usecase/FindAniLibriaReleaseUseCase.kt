package su.afk.yummy.tv.source.anilibria.domain.usecase

import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaRelease
import su.afk.yummy.tv.source.anilibria.domain.repository.AniLibriaRepository
import javax.inject.Inject

/** Находит релиз AniLibria для тайтла по id MyAnimeList, перебирая названия тайтла; без точного совпадения id релиза нет. */
class FindAniLibriaReleaseUseCase @Inject constructor(
    private val repository: AniLibriaRepository,
) {
    suspend operator fun invoke(malId: Int, titles: List<String>): AniLibriaRelease? =
        repository.findRelease(malId, titles)
}
