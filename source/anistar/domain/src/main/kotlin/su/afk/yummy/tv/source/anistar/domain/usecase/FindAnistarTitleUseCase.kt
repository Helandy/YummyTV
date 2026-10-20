package su.afk.yummy.tv.source.anistar.domain.usecase

import su.afk.yummy.tv.source.anistar.domain.model.AnistarLookup
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTitle
import su.afk.yummy.tv.source.anistar.domain.repository.AnistarRepository
import javax.inject.Inject

/** Находит тайтл на AniStar по данным приложения (MAL id, названия, год, тип, число серий); `null` — тайтла на AniStar нет. */
class FindAnistarTitleUseCase @Inject constructor(
    private val repository: AnistarRepository,
) {
    suspend operator fun invoke(lookup: AnistarLookup): AnistarTitle? = repository.findTitle(lookup)
}
