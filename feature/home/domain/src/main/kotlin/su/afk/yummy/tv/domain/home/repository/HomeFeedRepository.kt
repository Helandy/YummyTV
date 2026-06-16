package su.afk.yummy.tv.domain.home.repository

import su.afk.yummy.tv.domain.home.model.HomeFeed

interface HomeFeedRepository {
    suspend fun getHomeFeed(): HomeFeed
    suspend fun getCachedHomeFeed(): HomeFeed?
    suspend fun refreshHomeFeed(): HomeFeed
    suspend fun removeCachedContinueWatching(animeId: Int)
}
