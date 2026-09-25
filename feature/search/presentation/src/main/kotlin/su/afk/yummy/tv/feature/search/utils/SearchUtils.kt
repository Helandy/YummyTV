package su.afk.yummy.tv.feature.search.utils

import su.afk.yummy.tv.domain.search.model.SearchFilters

/** Меняет местами годы, если «от» больше «до». */
internal fun SearchFilters.normalizedYears(): SearchFilters {
    val from = fromYear
    val to = toYear
    return if (from != null && to != null && from > to) {
        copy(fromYear = to, toYear = from)
    } else {
        this
    }
}

internal fun <T> Set<T>.toggle(value: T): Set<T> =
    if (value in this) this - value else this + value
