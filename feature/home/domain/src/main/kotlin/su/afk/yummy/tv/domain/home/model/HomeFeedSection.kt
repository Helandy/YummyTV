package su.afk.yummy.tv.domain.home.model

data class HomeFeedSection(
    val type: HomeFeedSectionType,
    val title: String,
    val items: List<HomeFeedItem>,
)
