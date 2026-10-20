package su.afk.yummy.tv.source.aniboom.domain.usecase

import su.afk.yummy.tv.source.aniboom.domain.model.AniboomLookup
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTitle
import su.afk.yummy.tv.source.aniboom.domain.repository.AniboomRepository
import javax.inject.Inject

/** Находит тайтл в AniBoom по данным приложения (MAL id, названия, год, тип, число серий); `null` — тайтла в AniBoom нет. */
class FindAniboomTitleUseCase @Inject constructor(
    private val repository: AniboomRepository,
) {
    suspend operator fun invoke(lookup: AniboomLookup): AniboomTitle? = repository.findTitle(lookup)
}
