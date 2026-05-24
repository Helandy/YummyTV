package su.afk.yummy.tv.domain.search

data class SearchFilters(
    val genres: Set<String> = emptySet(),
    val excludedGenres: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val statuses: Set<String> = emptySet(),
    val fromYear: Int? = null,
    val toYear: Int? = null,
    val seasons: Set<String> = emptySet(),
    val ageRatings: Set<Int> = emptySet(),
    val sort: SearchSort = SearchSort.RELEVANCE,
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
            if (sort != SearchSort.RELEVANCE) count += 1
            if (!sortForward) count += 1
            return count
        }

    companion object {
        val EMPTY = SearchFilters()
    }
}

enum class SearchSort(val apiValue: String?) {
    RELEVANCE(null),
    TITLE("title"),
    YEAR("year"),
    RATING("rating"),
    RATING_COUNTERS("rating_counters"),
    VIEWS("views"),
    TOP("top"),
    RANDOM("random"),
    ID("id"),
}

data class SearchFilterOptions(
    val genreGroups: List<SearchGenreGroup> = emptyList(),
    val genres: List<SearchGenre> = emptyList(),
    val types: List<SearchAnimeType> = emptyList(),
)

data class SearchGenreGroup(
    val id: Int,
    val title: String,
)

data class SearchGenre(
    val id: String,
    val title: String,
    val groupId: Int,
)

data class SearchAnimeType(
    val id: String,
    val title: String,
)
