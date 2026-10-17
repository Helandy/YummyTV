package su.afk.yummy.tv.core.storage.watchlater

import kotlinx.coroutines.flow.Flow

/** Абстракция над локальным хранилищем отложенных серий — позволяет подменять реализацию в тестах. */
interface WatchLaterStorage {

    /** Отложенные серии, свежие сверху. */
    fun observeAll(): Flow<List<WatchLaterEntry>>

    /** Отложенные серии одного тайтла. */
    fun observeByAnimeId(animeId: Int): Flow<List<WatchLaterEntry>>

    suspend fun save(
        animeId: Int,
        episode: String,
        animeTitle: String = "",
        posterUrl: String = "",
        screenshotUrl: String = "",
        addedAt: Long = System.currentTimeMillis(),
    )

    suspend fun delete(animeId: Int, episode: String)

    suspend fun deleteByAnimeId(animeId: Int)
}
