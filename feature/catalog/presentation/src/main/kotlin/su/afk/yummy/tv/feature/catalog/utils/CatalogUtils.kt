package su.afk.yummy.tv.feature.catalog.utils

import su.afk.yummy.tv.domain.catalog.model.SearchFilters

/**
 * Отбрасывает год вне диапазона, который принимает сервер (иначе 400 на весь поиск — например,
 * опечатка «2926» с пульта), и меняет местами годы, если «от» больше «до».
 */
internal fun SearchFilters.normalizedYears(): SearchFilters {
    val from = fromYear?.takeIf { it in SEARCH_YEAR_RANGE }
    val to = toYear?.takeIf { it in SEARCH_YEAR_RANGE }
    return if (from != null && to != null && from > to) {
        copy(fromYear = to, toYear = from)
    } else {
        copy(fromYear = from, toYear = to)
    }
}

/** Диапазон `from_year`/`to_year`, который принимает yani `/anime`. */
private val SEARCH_YEAR_RANGE = 1900..2100

internal fun <T> Set<T>.toggle(value: T): Set<T> =
    if (value in this) this - value else this + value

/** Выбор одного значения, как в списках сайта: повторный выбор снимает его, новый заменяет прежний. */
internal fun <T> Set<T>.toggleSingle(value: T): Set<T> =
    if (value in this) emptySet() else setOf(value)
