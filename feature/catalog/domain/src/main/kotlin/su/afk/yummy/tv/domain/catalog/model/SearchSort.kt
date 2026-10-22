package su.afk.yummy.tv.domain.catalog.model

enum class SearchSort(val apiValue: String, val defaultForward: Boolean) {
    TITLE("title", false),
    RATING("rating", false),
    RATING_COUNTERS("rating_counters", false),
    VIEWS("views", false),
    TOP("top", true),
    ID("id", false),
    YEAR("year", false),
    ;

    companion object {
        /** Сортировки для выбора в UI, в порядке сайта. */
        val options: List<SearchSort> = listOf(TOP, RATING, ID, TITLE, VIEWS, RATING_COUNTERS, YEAR)
    }
}
