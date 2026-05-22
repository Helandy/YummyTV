package su.afk.yummy.tv.domain.home

interface HomeFeedRepository {
    suspend fun getHomeFeed(): HomeFeed
}
