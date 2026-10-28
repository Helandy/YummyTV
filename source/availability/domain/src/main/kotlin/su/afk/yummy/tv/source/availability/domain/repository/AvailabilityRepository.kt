package su.afk.yummy.tv.source.availability.domain.repository

import su.afk.yummy.tv.source.availability.domain.model.AvailabilityAnswer
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup

interface AvailabilityRepository {
    /** Один запрос к серверу на все балансеры; одновременные запросы по одному тайтлу делят результат. Сбой сети уходит исключением. */
    suspend fun find(lookup: AvailabilityLookup): AvailabilityAnswer
}
