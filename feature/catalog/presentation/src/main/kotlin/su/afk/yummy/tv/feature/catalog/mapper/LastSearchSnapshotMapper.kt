package su.afk.yummy.tv.feature.catalog.mapper

import su.afk.yummy.tv.core.model.settings.LastSearchSnapshot
import su.afk.yummy.tv.domain.catalog.model.SearchFilters
import su.afk.yummy.tv.domain.catalog.model.SearchSort

internal fun SearchFilters.toLastSearchSnapshot(query: String) = LastSearchSnapshot(
    query = query,
    genres = genres,
    excludedGenres = excludedGenres,
    types = types,
    statuses = statuses,
    fromYear = fromYear,
    toYear = toYear,
    seasons = seasons,
    ageRatings = ageRatings,
    sortName = sort.name,
    sortForward = sortForward,
)

// Тип, статус, сезон и возраст — одиночный выбор: старый снимок с несколькими значениями сводим к первому
internal fun LastSearchSnapshot.toSearchFilters() = SearchFilters(
    genres = genres,
    excludedGenres = excludedGenres,
    types = types.take(1).toSet(),
    statuses = statuses.take(1).toSet(),
    fromYear = fromYear,
    toYear = toYear,
    seasons = seasons.take(1).toSet(),
    ageRatings = ageRatings.take(1).toSet(),
    sort = SearchSort.entries.firstOrNull { it.name == sortName } ?: SearchSort.TOP,
    sortForward = sortForward,
)
