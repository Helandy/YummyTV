package su.afk.yummy.tv.domain.top100

data class AnimeTopPage(
    val items: List<AnimeTopItem>,
    val nextOffset: Int,
    val canLoadMore: Boolean,
)
