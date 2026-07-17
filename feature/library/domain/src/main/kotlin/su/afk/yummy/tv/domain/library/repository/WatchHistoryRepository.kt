package su.afk.yummy.tv.domain.library.repository

import su.afk.yummy.tv.domain.library.model.WatchHistoryEntry

interface WatchHistoryRepository {
    suspend fun getPage(limit: Int, offset: Int): List<WatchHistoryEntry>
}
