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

        fun isContinueTarget(positionMs: Long, durationMs: Long): Boolean =
            positionMs == 0L && durationMs == 0L

        fun isUnresolvedProgress(positionMs: Long, durationMs: Long): Boolean =
            durationMs == 0L && positionMs >= MIN_CONTINUE_WATCHING_POSITION_MS

        fun isWatchedProgress(positionMs: Long, durationMs: Long): Boolean =
            isMeaningfulProgress(positionMs, durationMs) &&
                    progress(positionMs, durationMs) >= WATCHED_PROGRESS

        fun isMeaningfulProgressEntry(entry: WatchProgressEntry): Boolean =
            isMeaningfulProgress(entry.positionMs, entry.durationMs)

        fun isContinueTargetEntry(entry: WatchProgressEntry): Boolean =
            isContinueTarget(entry.positionMs, entry.durationMs) &&
                    entry.episode.isNotBlank() &&
                    entry.episodeUrl.isNotBlank()

        fun hasPlayableTargetEntry(entry: WatchProgressEntry): Boolean =
            entry.videoId > 0 ||
                    entry.episode.isNotBlank() ||
                    entry.episodeUrl.isNotBlank()

        fun isUnresolvedProgressEntry(entry: WatchProgressEntry): Boolean =
            isUnresolvedProgress(entry.positionMs, entry.durationMs) &&
                    hasPlayableTargetEntry(entry)

        fun isWatchedProgressEntry(entry: WatchProgressEntry): Boolean =
            isWatchedProgress(entry.positionMs, entry.durationMs)

        fun isContinueWatchingEntry(entry: WatchProgressEntry): Boolean =
            isContinueTargetEntry(entry) ||
                    isUnresolvedProgressEntry(entry) ||
                    (isMeaningfulProgressEntry(entry) && !isWatchedProgressEntry(entry))

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

    suspend fun allMeaningfulVideoProgress(): List<WatchProgressEntry> =
        dao.allMeaningfulVideoProgress(
            minPositionMs = MIN_CONTINUE_WATCHING_POSITION_MS,
        )

    suspend fun suppressedContinueWatchingAnimeIds(): Set<Int> =
        dao.suppressedAnimeIds().toSet()

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
            val existing = dao.get(animeId, episode)
            if (existing != null && isContinueTargetEntry(existing)) {
                dao.deleteSuppression(animeId)
                dao.save(
                    existing.copy(
                        videoId = videoId.takeIf { it > 0 } ?: existing.videoId,
                        episodeUrl = episodeUrl.ifBlank { existing.episodeUrl },
                        animeTitle = animeTitle.ifBlank { existing.animeTitle },
                        posterUrl = posterUrl.ifBlank { existing.posterUrl },
                        playerName = playerName.ifBlank { existing.playerName },
                        dubbing = dubbing.ifBlank { existing.dubbing },
                        screenshotUrl = screenshotUrl.ifBlank { existing.screenshotUrl },
                        updatedAt = System.currentTimeMillis(),
                    )
                )
                return
            }
            dao.delete(animeId, episode)
            return
        }
        dao.deleteSuppression(animeId)
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

    suspend fun saveContinueTarget(
        animeId: Int,
        episode: String,
        videoId: Int = 0,
        episodeUrl: String,
        animeTitle: String = "",
        posterUrl: String = "",
        playerName: String = "",
        dubbing: String = "",
        screenshotUrl: String = "",
    ) {
        if (animeId <= 0 || episode.isBlank() || episodeUrl.isBlank()) return
        dao.deleteSuppression(animeId)
        dao.save(
            WatchProgressEntry(
                animeId = animeId,
                episode = episode,
                videoId = videoId,
                episodeUrl = episodeUrl,
                positionMs = 0L,
                durationMs = 0L,
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

    suspend fun suppressContinueWatching(animeId: Int) {
        dao.saveSuppression(ContinueWatchingSuppressionEntry(animeId = animeId))
        dao.deleteByAnimeId(animeId)
    }

    suspend fun suppressContinueWatchingDisplay(animeId: Int) {
        if (animeId <= 0) return
        dao.saveSuppression(ContinueWatchingSuppressionEntry(animeId = animeId))
    }
}
