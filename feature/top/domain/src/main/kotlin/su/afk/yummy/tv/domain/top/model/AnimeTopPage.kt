package su.afk.yummy.tv.domain.top.model

data class AnimeTopPage(
    val items: List<AnimeTopItem>,
    val nextOffset: Int,
    val canLoadMore: Boolean,
)
