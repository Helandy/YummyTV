package su.afk.yummy.tv.core.storage.watchprogress

import kotlinx.coroutines.flow.Flow

class WatchProgressStore(private val dao: WatchProgressDao) {

    suspend fun get(animeId: Int, episode: String): WatchProgressEntry? = dao.get(animeId, episode)

    fun observeAll(): Flow<List<WatchProgressEntry>> = dao.observeAll()

    suspend fun save(
        animeId: Int,
        episode: String,
        episodeUrl: String,
        positionMs: Long,
        durationMs: Long,
        animeTitle: String = "",
        posterUrl: String = "",
        playerName: String = "",
        dubbing: String = "",
        screenshotUrl: String = "",
    ) = dao.save(WatchProgressEntry(animeId = animeId, episode = episode, episodeUrl = episodeUrl, positionMs = positionMs, durationMs = durationMs, animeTitle = animeTitle, posterUrl = posterUrl, playerName = playerName, dubbing = dubbing, screenshotUrl = screenshotUrl))

    suspend fun delete(animeId: Int, episode: String) = dao.delete(animeId, episode)

    suspend fun deleteByAnimeId(animeId: Int) = dao.deleteByAnimeId(animeId)
}
