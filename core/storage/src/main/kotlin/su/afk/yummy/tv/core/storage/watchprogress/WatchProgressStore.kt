package su.afk.yummy.tv.core.storage.watchprogress

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore.Companion.isWatchedProgressEntry

class WatchProgressStore(private val dao: WatchProgressDao) {

    private val writeMutex = Mutex()

    companion object {
        const val MIN_CONTINUE_WATCHING_POSITION_MS = 30_000L
        const val WATCHED_REMAINING_MS = 5 * 60 * 1000L
        const val SHORT_EPISODE_WATCHED_PROGRESS = 0.90f

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

        fun isWatchedProgress(positionMs: Long, durationMs: Long): Boolean {
            if (!isMeaningfulProgress(positionMs, durationMs)) return false
            return if (durationMs <= WATCHED_REMAINING_MS) {
                progress(positionMs, durationMs) >= SHORT_EPISODE_WATCHED_PROGRESS
            } else {
                positionMs >= durationMs - WATCHED_REMAINING_MS
            }
        }

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

    fun observeByAnimeId(animeId: Int): Flow<List<WatchProgressEntry>> =
        dao.observeByAnimeId(animeId)

    fun observeContinueWatching(): Flow<List<WatchProgressEntry>> =
        combine(
            dao.observeContinueWatching(
                minPositionMs = MIN_CONTINUE_WATCHING_POSITION_MS,
            ),
            observeWatchedProgress(),
        ) { candidates, watchedEntries ->
            ContinueWatchingMerge.filterDisplayable(candidates + watchedEntries)
        }

    suspend fun continueWatching(): List<WatchProgressEntry> {
        val candidates = dao.continueWatching(
            minPositionMs = MIN_CONTINUE_WATCHING_POSITION_MS,
        )
        return ContinueWatchingMerge.filterDisplayable(candidates + watchedProgress())
    }

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

    suspend fun continueWatchingSuppressionTimestamps(): Map<Int, Long> =
        dao.suppressions().associate { it.animeId to it.suppressedAt }

    fun observeContinueWatchingSuppressionTimestamps(): Flow<Map<Int, Long>> =
        dao.observeSuppressions().map { entries ->
            entries.associate { it.animeId to it.suppressedAt }
        }

    suspend fun watchedProgress(): List<WatchProgressEntry> =
        dao.watchedProgress(
            minPositionMs = MIN_CONTINUE_WATCHING_POSITION_MS,
        ).filter(WatchProgressStore::isWatchedProgressEntry)

    fun observeWatchedProgress(): Flow<List<WatchProgressEntry>> =
        dao.observeAll().map { entries ->
            entries
                .filter(WatchProgressStore::isWatchedProgressEntry)
        }

    /**
     * Сохраняет фактическую позицию просмотра серии.
     *
     * Позиции меньше минимального порога не создают отдельный прогресс. Уже созданная цель
     * продолжения `0:00` обновляет метаданные, а существующая значимая запись сохраняется без
     * изменений: временный ноль от lifecycle/Media3 не должен удалять Continue Watching.
     * Запись синхронизирована с [saveContinueTarget], чтобы параллельные события плеера не
     * перетирали более актуальное состояние серии.
     */
    suspend fun save(
        animeId: Int,
        episode: String,
        videoId: Int = 0,
        episodeUrl: String,
        positionMs: Long,
        durationMs: Long,
        updatedAt: Long = System.currentTimeMillis(),
        animeTitle: String = "",
        posterUrl: String = "",
        playerName: String = "",
        dubbing: String = "",
        screenshotUrl: String = "",
    ) = writeMutex.withLock {
        val existing = dao.get(animeId, episode)
        if (positionMs < MIN_CONTINUE_WATCHING_POSITION_MS) {
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
                        updatedAt = updatedAt,
                    )
                )
                return@withLock
            }
            if (existing != null && isMeaningfulProgressEntry(existing)) {
                return@withLock
            }
            dao.delete(animeId, episode)
            return@withLock
        }
        dao.deleteSuppression(animeId)
        dao.save(
            WatchProgressEntry(
                animeId = animeId,
                episode = episode,
                videoId = videoId.takeIf { it > 0 } ?: existing?.videoId ?: 0,
                episodeUrl = episodeUrl.ifBlank { existing?.episodeUrl.orEmpty() },
                positionMs = positionMs,
                durationMs = durationMs.takeIf { it > 0L } ?: existing?.durationMs ?: 0L,
                updatedAt = updatedAt,
                animeTitle = animeTitle.ifBlank { existing?.animeTitle.orEmpty() },
                posterUrl = posterUrl.ifBlank { existing?.posterUrl.orEmpty() },
                playerName = playerName.ifBlank { existing?.playerName.orEmpty() },
                dubbing = dubbing.ifBlank { existing?.dubbing.orEmpty() },
                screenshotUrl = screenshotUrl.ifBlank { existing?.screenshotUrl.orEmpty() },
            )
        )
    }

    /**
     * Делает серию следующей целью Continue Watching.
     *
     * Если у серии есть недосмотренный прогресс, сохраняет его позицию и обновляет только
     * источник, метаданные и время активности — плеер продолжит с сохранённого места. Если серия
     * уже досмотрена по общему правилу [isWatchedProgressEntry], либо прогресса ещё нет, записывает
     * новую цель `0:00`.
     */
    suspend fun saveContinueTarget(
        animeId: Int,
        episode: String,
        videoId: Int = 0,
        episodeUrl: String,
        updatedAt: Long = System.currentTimeMillis(),
        animeTitle: String = "",
        posterUrl: String = "",
        playerName: String = "",
        dubbing: String = "",
        screenshotUrl: String = "",
    ) {
        if (animeId <= 0 || episode.isBlank() || episodeUrl.isBlank()) return
        writeMutex.withLock {
            val existing = dao.get(animeId, episode)
            dao.deleteSuppression(animeId)
            dao.save(
                if (existing != null && !isContinueTargetEntry(existing) &&
                    !isWatchedProgressEntry(existing)
                ) {
                    existing.copy(
                        videoId = videoId.takeIf { it > 0 } ?: existing.videoId,
                        episodeUrl = episodeUrl.ifBlank { existing.episodeUrl },
                        updatedAt = updatedAt,
                        animeTitle = animeTitle.ifBlank { existing.animeTitle },
                        posterUrl = posterUrl.ifBlank { existing.posterUrl },
                        playerName = playerName.ifBlank { existing.playerName },
                        dubbing = dubbing.ifBlank { existing.dubbing },
                        screenshotUrl = screenshotUrl.ifBlank { existing.screenshotUrl },
                    )
                } else {
                    WatchProgressEntry(
                        animeId = animeId,
                        episode = episode,
                        videoId = videoId.takeIf { it > 0 } ?: existing?.videoId ?: 0,
                        episodeUrl = episodeUrl.ifBlank { existing?.episodeUrl.orEmpty() },
                        positionMs = 0L,
                        durationMs = 0L,
                        updatedAt = updatedAt,
                        animeTitle = animeTitle.ifBlank { existing?.animeTitle.orEmpty() },
                        posterUrl = posterUrl.ifBlank { existing?.posterUrl.orEmpty() },
                        playerName = playerName.ifBlank { existing?.playerName.orEmpty() },
                        dubbing = dubbing.ifBlank { existing?.dubbing.orEmpty() },
                        screenshotUrl = screenshotUrl.ifBlank { existing?.screenshotUrl.orEmpty() },
                    )
                }
            )
        }
    }

    suspend fun delete(animeId: Int, episode: String) = dao.delete(animeId, episode)

    suspend fun deleteByAnimeId(animeId: Int) = dao.deleteByAnimeId(animeId)

    suspend fun suppressContinueWatching(animeId: Int) {
        dao.saveSuppression(ContinueWatchingSuppressionEntry(animeId = animeId))
        dao.deleteByAnimeId(animeId)
    }

    suspend fun suppressContinueWatchingDisplay(
        animeId: Int,
        suppressedAt: Long = System.currentTimeMillis(),
    ) {
        if (animeId <= 0) return
        dao.saveSuppression(
            ContinueWatchingSuppressionEntry(
                animeId = animeId,
                suppressedAt = suppressedAt,
            )
        )
    }

}
