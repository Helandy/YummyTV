package su.afk.yummy.tv.source.availability.domain.usecase

import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.repository.AvailabilityRepository
import java.io.IOException
import javax.inject.Inject

/**
 * Находит тайтл у балансера [sourceId] через общую ручку наличия; `null` — у балансера тайтла нет. Если сервер не смог
 * опросить сам балансер, это сбой ([IOException]), а не «тайтла нет»: источник не должен пропасть из кэша выдачи.
 */
class FindSourceAvailabilityUseCase @Inject constructor(
    private val repository: AvailabilityRepository,
) {
    suspend operator fun invoke(sourceId: String, lookup: AvailabilityLookup): SourceAvailability? {
        val answer = repository.find(lookup)
        answer.sources.firstOrNull { it.sourceId == sourceId }?.let { return it }
        if (sourceId in answer.failedSourceIds) throw IOException("Availability: источник $sourceId не ответил")
        return null
    }
}
