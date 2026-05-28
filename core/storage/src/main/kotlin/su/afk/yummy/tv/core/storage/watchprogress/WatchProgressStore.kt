package su.afk.yummy.tv.core.storage.watchprogress

import kotlinx.coroutines.flow.Flow

class WatchProgressStore(private val dao: WatchProgressDao) {

    companion object {
        const val MIN_CONTINUE_WATCHING_POSITION_MS = 30_000L
        private const val MAX_CONTINUE_WATCHING_PROGRESS = 0.90f

        fun isContinueWatchingEntry(entry: WatchProgressEntry): Boolean =
            entry.durationMs > 0 &&
                entry.positionMs >= MIN_CONTINUE_WATCHING_POSITION_MS &&
                entry.positionMs.toFloat() / entry.durationMs < MAX_CONTINUE_WATCHING_PROGRESS
    }

    suspend fun get(animeId: Int, episode: String): WatchProgressEntry? = dao.get(animeId, episode)

    fun observeAll(): Flow<List<WatchProgressEntry>> = dao.observeAll()

    fun observeByAnimeId(animeId: Int): Flow<List<WatchProgressEntry>> = dao.observeByAnimeId(animeId)

    fun observeContinueWatching(): Flow<List<WatchProgressEntry>> =
        dao.observeContinueWatching(
            minPositionMs = MIN_CONTINUE_WATCHING_POSITION_MS,
            maxProgress = MAX_CONTINUE_WATCHING_PROGRESS,
        )

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
