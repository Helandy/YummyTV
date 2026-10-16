package su.afk.yummy.tv.domain.catalog.model

data class SearchFilters(
    val genres: Set<String> = emptySet(),
    val excludedGenres: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val statuses: Set<String> = emptySet(),
    val fromYear: Int? = null,
    val toYear: Int? = null,
    val seasons: Set<String> = emptySet(),
    val ageRatings: Set<Int> = emptySet(),
    val sort: SearchSort = SearchSort.TOP,
    val sortForward: Boolean = true,
) {
    val isEmpty: Boolean
        get() = this == EMPTY

    val activeCount: Int
        get() {
            var count = genres.size +
                excludedGenres.size +
                types.size +
                statuses.size +
                seasons.size +
                ageRatings.size +
                listOfNotNull(fromYear, toYear).size
            // Сортировка по умолчанию (топ по возрастанию, как на сайте) не считается фильтром
            if (sort != SearchSort.TOP || !sortForward) count += 1
            return count
        }

    companion object {
        val EMPTY = SearchFilters()
    }
}
