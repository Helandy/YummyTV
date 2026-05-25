package su.afk.yummy.tv.domain.search.model

data class SearchPage(
    val items: List<SearchItem>,
    val nextOffset: Int,
    val canLoadMore: Boolean,
)
