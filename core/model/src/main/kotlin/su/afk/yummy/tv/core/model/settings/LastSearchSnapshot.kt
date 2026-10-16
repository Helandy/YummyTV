package su.afk.yummy.tv.core.model.settings

/** Снимок последнего поиска (запрос + фильтры) для настройки «сохранять последний поиск». */
data class LastSearchSnapshot(
    val query: String = "",
    val genres: Set<String> = emptySet(),
    val excludedGenres: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val statuses: Set<String> = emptySet(),
    val seasons: Set<String> = emptySet(),
    val ageRatings: Set<Int> = emptySet(),
    val fromYear: Int? = null,
    val toYear: Int? = null,
    val sortName: String = "TOP",
    val sortForward: Boolean = true,
) {
    val isEmpty: Boolean get() = this == LastSearchSnapshot()
}
