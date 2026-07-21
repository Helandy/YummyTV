package su.afk.yummy.tv.feature.search.utils

import su.afk.yummy.tv.domain.search.model.SearchFilters

internal fun SearchFilters.focusStateKey(): String = buildString {
    append("genres=")
    append(genres.sorted().joinToString(","))
    append("|excluded=")
    append(excludedGenres.sorted().joinToString(","))
    append("|types=")
    append(types.sorted().joinToString(","))
    append("|statuses=")
    append(statuses.sorted().joinToString(","))
    append("|from=")
    append(fromYear)
    append("|to=")
    append(toYear)
    append("|seasons=")
    append(seasons.sorted().joinToString(","))
    append("|ages=")
    append(ageRatings.sorted().joinToString(","))
    append("|sort=")
    append(sort.name)
    append("|forward=")
    append(sortForward)
}

internal fun Throwable.uiMessage(): String =
    message ?: localizedMessage ?: toString()
