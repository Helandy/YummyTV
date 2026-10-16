package su.afk.yummy.tv.domain.catalog.model

data class SearchPage(
    val items: List<SearchItem>,
    val nextOffset: Int,
    val canLoadMore: Boolean,
)
