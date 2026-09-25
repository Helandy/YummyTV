package su.afk.yummy.tv.feature.search.mapper

import su.afk.yummy.tv.core.model.settings.LastSearchSnapshot
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchSort

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

internal fun LastSearchSnapshot.toSearchFilters() = SearchFilters(
    genres = genres,
    excludedGenres = excludedGenres,
    types = types,
    statuses = statuses,
    fromYear = fromYear,
    toYear = toYear,
    seasons = seasons,
    ageRatings = ageRatings,
    sort = SearchSort.entries.firstOrNull { it.name == sortName } ?: SearchSort.RELEVANCE,
    sortForward = sortForward,
)
