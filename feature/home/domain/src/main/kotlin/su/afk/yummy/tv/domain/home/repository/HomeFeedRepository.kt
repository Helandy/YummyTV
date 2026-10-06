package su.afk.yummy.tv.domain.home.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.home.model.ContinueWatchingProgressMigration
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed

interface HomeFeedRepository {
    suspend fun getHomeFeed(): HomeFeed
    suspend fun getCachedHomeFeed(): HomeFeed?
    suspend fun refreshHomeFeed(): HomeFeed
    suspend fun removeCachedContinueWatching(animeId: Int)
    suspend fun getContinueWatchingVideoIds(animeId: Int): List<Int>
    suspend fun migrateContinueWatchingProgress(migration: ContinueWatchingProgressMigration)
    fun observeContinueWatching(): Flow<List<HomeContinueWatchingItem>>

    /**
     * Просмотренные серии по тайтлам: id тайтла → номера серий. Номера нормализованы, потому что
     * озвучки присылают их по-разному («01» и «1» — одна и та же серия).
     */
    fun observeWatchedEpisodes(): Flow<Map<Int, Set<Int>>>
}
