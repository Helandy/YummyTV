package su.afk.yummy.tv.source.amedia.domain.usecase

import su.afk.yummy.tv.source.amedia.domain.model.AmediaLookup
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTitle
import su.afk.yummy.tv.source.amedia.domain.repository.AmediaRepository
import javax.inject.Inject

/** Находит тайтл на amd.online по данным приложения (MAL id, названия, год, тип, число серий); `null` — тайтла на сайте нет. */
class FindAmediaTitleUseCase @Inject constructor(
    private val repository: AmediaRepository,
) {
    suspend operator fun invoke(lookup: AmediaLookup): AmediaTitle? = repository.findTitle(lookup)
}
