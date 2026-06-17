package su.afk.yummy.tv.domain.collection.model

data class CollectionSummaryPage(
    val items: List<CollectionSummary>,
    val nextOffset: Int,
    val canLoadMore: Boolean,
)
