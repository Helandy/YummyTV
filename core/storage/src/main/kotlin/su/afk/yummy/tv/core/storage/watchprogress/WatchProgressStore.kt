package su.afk.yummy.tv.core.storage.watchprogress

import kotlinx.coroutines.flow.Flow

class WatchProgressStore(private val dao: WatchProgressDao) {

    companion object {
        const val MIN_CONTINUE_WATCHING_POSITION_MS = 30_000L
        const val WATCHED_PROGRESS = 0.90f

        fun progress(positionMs: Long, durationMs: Long): Float =
            if (durationMs > 0) {
                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

        fun isMeaningfulProgress(positionMs: Long, durationMs: Long): Boolean =
            durationMs > 0 && positionMs >= MIN_CONTINUE_WATCHING_POSITION_MS

        fun isWatchedProgress(positionMs: Long, durationMs: Long): Boolean =
            isMeaningfulProgress(positionMs, durationMs) &&
                    progress(positionMs, durationMs) >= WATCHED_PROGRESS

        fun isMeaningfulProgressEntry(entry: WatchProgressEntry): Boolean =
            isMeaningfulProgress(entry.positionMs, entry.durationMs)

        fun isWatchedProgressEntry(entry: WatchProgressEntry): Boolean =
            isWatchedProgress(entry.positionMs, entry.durationMs)

        fun isContinueWatchingEntry(entry: WatchProgressEntry): Boolean =
            isMeaningfulProgressEntry(entry) && !isWatchedProgressEntry(entry)

        fun latestByAnime(entries: List<WatchProgressEntry>): List<WatchProgressEntry> =
            entries
                .groupBy { it.animeId }
                .values
                .map { group -> group.maxBy { it.updatedAt } }
                .sortedByDescending { it.updatedAt }
    }

    suspend fun get(animeId: Int, episode: String): WatchProgressEntry? = dao.get(animeId, episode)

    fun observeAll(): Flow<List<WatchProgressEntry>> = dao.observeAll()

    fun observeByAnimeId(animeId: Int): Flow<List<WatchProgressEntry>> = dao.observeByAnimeId(animeId)

    fun observeContinueWatching(): Flow<List<WatchProgressEntry>> =
        dao.observeContinueWatching(
            minPositionMs = MIN_CONTINUE_WATCHING_POSITION_MS,
            maxProgress = WATCHED_PROGRESS,
        )

    suspend fun continueWatching(): List<WatchProgressEntry> =
        dao.continueWatching(
            minPositionMs = MIN_CONTINUE_WATCHING_POSITION_MS,
            maxProgress = WATCHED_PROGRESS,
        )

    suspend fun latestMeaningfulVideoProgress(limit: Int): List<WatchProgressEntry> =
        dao.latestMeaningfulVideoProgress(
            minPositionMs = MIN_CONTINUE_WATCHING_POSITION_MS,
            limit = limit,
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
