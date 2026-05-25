package su.afk.yummy.tv.domain.home

data class HomeFeed(
    val heroItems: List<HomeFeedItem>,
    val sections: List<HomeFeedSection>,
)
