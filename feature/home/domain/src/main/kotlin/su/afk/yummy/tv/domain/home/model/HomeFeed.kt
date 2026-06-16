package su.afk.yummy.tv.domain.home.model

data class HomeFeed(
    val continueWatchingItems: List<HomeContinueWatchingItem> = emptyList(),
    val heroItems: List<HomeFeedItem>,
    val sections: List<HomeFeedSection>,
)
