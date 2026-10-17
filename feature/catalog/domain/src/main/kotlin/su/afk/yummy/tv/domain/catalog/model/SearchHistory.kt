package su.afk.yummy.tv.domain.catalog.model

/** Сколько последних запросов хранит история поиска. */
const val SEARCH_HISTORY_LIMIT = 10

/**
 * История с [query] в начале: пробелы по краям обрезаны, прежний такой же запрос (без учёта
 * регистра) убран, длина не больше [limit]. Пустой запрос историю не меняет.
 */
fun List<String>.withSearchQuery(query: String, limit: Int = SEARCH_HISTORY_LIMIT): List<String> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return this
    return (listOf(normalized) + filterNot { it.equals(normalized, ignoreCase = true) }).take(limit)
}
