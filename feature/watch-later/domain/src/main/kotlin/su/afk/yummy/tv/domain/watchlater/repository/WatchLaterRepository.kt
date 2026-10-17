package su.afk.yummy.tv.domain.watchlater.repository

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.watchlater.model.WatchLaterItem

interface WatchLaterRepository {

    fun observeAll(): Flow<List<WatchLaterItem>>

    fun observeEpisodes(animeId: Int): Flow<Set<String>>

    suspend fun add(item: WatchLaterItem)

    suspend fun remove(animeId: Int, episode: String)
}
