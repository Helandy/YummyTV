package su.afk.yummy.tv.core.storage.watchprogress

import kotlinx.coroutines.flow.Flow

class WatchProgressStore(private val dao: WatchProgressDao) {

    companion object {
        const val MIN_CONTINUE_WATCHING_POSITION_MS = 30_000L

        fun isContinueWatchingEntry(entry: WatchProgressEntry): Boolean =
            entry.durationMs > 0 &&
                entry.positionMs >= MIN_CONTINUE_WATCHING_POSITION_MS &&
                entry.positionMs.toFloat() / entry.durationMs < 0.90f
    }

    suspend fun get(animeId: Int, episode: String): WatchProgressEntry? = dao.get(animeId, episode)

    fun observeAll(): Flow<List<WatchProgressEntry>> = dao.observeAll()

    suspend fun save(
        animeId: Int,
        episode: String,
        videoId: Int = 0,
        episodeUrl: String,
        positionMs: Long,
        durationMs: Long,
        animeTitle: String = "",
        posterUrl: String = "",
        playerName: String = "",
        dubbing: String = "",
        screenshotUrl: String = "",
    ) {
        if (positionMs < MIN_CONTINUE_WATCHING_POSITION_MS) {
            dao.delete(animeId, episode)
            return
        }
        dao.save(
            WatchProgressEntry(
                animeId = animeId,
                episode = episode,
                videoId = videoId,
                episodeUrl = episodeUrl,
                positionMs = positionMs,
                durationMs = durationMs,
                animeTitle = animeTitle,
                posterUrl = posterUrl,
                playerName = playerName,
                dubbing = dubbing,
                screenshotUrl = screenshotUrl,
            )
        )
    }

    suspend fun delete(animeId: Int, episode: String) = dao.delete(animeId, episode)

    suspend fun deleteByAnimeId(animeId: Int) = dao.deleteByAnimeId(animeId)
}
