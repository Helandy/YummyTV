package su.afk.yummy.tv.domain.search

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
