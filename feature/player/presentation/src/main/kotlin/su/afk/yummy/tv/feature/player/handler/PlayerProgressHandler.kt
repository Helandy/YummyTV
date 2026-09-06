package su.afk.yummy.tv.feature.player.handler

import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.model.anime.isMeaningfulProgress
import su.afk.yummy.tv.core.model.anime.isWatchedProgress
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.account.usecase.SaveVideoWatchProgressUseCase
import su.afk.yummy.tv.domain.player.repository.WatchProgressRepository
import su.afk.yummy.tv.feature.player.model.PlayerProgressSnapshot
import su.afk.yummy.tv.feature.player.utils.withFullTimingIfWatched
import javax.inject.Inject

private const val REMOTE_PROGRESS_SYNC_INTERVAL_MS = 10_000L

/** Хвост эпизода, который на сервер не отправляем как позицию/секунду (как в веб-клиенте). */
private const val WATCH_END_TOLERANCE_SECONDS = 10

/** Сохраняет локальный прогресс просмотра и тихо синхронизирует его с сервером. */
internal class PlayerProgressHandler @Inject constructor(
    private val watchProgressRepository: WatchProgressRepository,
    private val settingsStore: SettingsStore,
    private val saveVideoWatchProgress: SaveVideoWatchProgressUseCase,
) {
    private val completedRemoteVideoIds = mutableSetOf<Int>()
    private val completionAttemptedVideoIds = mutableSetOf<Int>()
    private val syncingRemoteVideoIds = mutableSetOf<Int>()
    private val lastRemoteSyncAttemptAt = mutableMapOf<Int, Long>()

    /** Все уникальные просмотренные секунды-позиции по videoId (что реально проиграли). */
    private val watchedSecondsByVideoId = mutableMapOf<Int, MutableSet<Int>>()

    /**
     * Секунды, уже успешно отправленные на сервер. Сервер СУММИРУЕТ присланные `times`,
     * поэтому каждую секунду шлём ровно один раз (дельта = watched − synced).
     */
    private val syncedSecondsByVideoId = mutableMapOf<Int, MutableSet<Int>>()

    /** Копит реально проигранную секунду (вызывается по тику ~1с из плеера). */
    fun recordWatchedSecond(videoId: Int, positionMs: Long, durationMs: Long) {
        if (videoId <= 0 || durationMs <= 0) return
        val maxSecond = ((durationMs / 1000L).toInt() - WATCH_END_TOLERANCE_SECONDS)
            .coerceAtLeast(0)
        val second = (positionMs / 1000L).toInt().coerceIn(0, maxSecond)
        watchedSecondsByVideoId.getOrPut(videoId) { sortedSetOf() }.add(second)
    }

    suspend fun saveProgress(
        context: PlayerProgressContext,
        snapshot: PlayerProgressSnapshot,
        forceRemoteSync: Boolean = false,
    ) {
        if (snapshot.durationMs <= 0) return
        val savedSnapshot = snapshot.withFullTimingIfWatched()

        if (context.animeId > 0 && savedSnapshot.episode.isNotBlank()) {
            val updatedAt = localActivityUpdatedAt(context.animeId, savedSnapshot.episode)
            watchProgressRepository.save(
                animeId = context.animeId,
                episode = savedSnapshot.episode,
                videoId = savedSnapshot.videoId,
                episodeUrl = savedSnapshot.episodeUrl,
                positionMs = savedSnapshot.positionMs,
                durationMs = savedSnapshot.durationMs,
                updatedAt = updatedAt,
                animeTitle = context.animeTitle,
                posterUrl = context.posterUrl,
                playerName = savedSnapshot.playerName,
                dubbing = savedSnapshot.dubbing,
                screenshotUrl = savedSnapshot.screenshotUrl,
            )
        }

        syncRemoteProgress(savedSnapshot, force = forceRemoteSync)
    }

    suspend fun saveContinueTarget(
        context: PlayerProgressContext,
        snapshot: PlayerProgressSnapshot,
    ) {
        val updatedAt = localActivityUpdatedAt(context.animeId, snapshot.episode)
        watchProgressRepository.saveContinueTarget(
            animeId = context.animeId,
            episode = snapshot.episode,
            videoId = snapshot.videoId,
            episodeUrl = snapshot.episodeUrl,
            updatedAt = updatedAt,
            animeTitle = context.animeTitle,
            posterUrl = context.posterUrl,
            playerName = snapshot.playerName,
            dubbing = snapshot.dubbing,
            screenshotUrl = snapshot.screenshotUrl,
        )
    }

    private suspend fun localActivityUpdatedAt(animeId: Int, episode: String): Long {
        val now = System.currentTimeMillis()
        if (animeId <= 0) return now

        val existingUpdatedAt = episode
            .takeIf { it.isNotBlank() }
            ?.let { watchProgressRepository.get(animeId, it)?.updatedAt }
            ?: 0L

        return maxOf(now, existingUpdatedAt + 1L)
    }

    suspend fun suppressContinueWatchingDisplay(context: PlayerProgressContext) {
        watchProgressRepository.suppressContinueWatchingDisplay(
            animeId = context.animeId,
            suppressedAt = System.currentTimeMillis(),
        )
    }

    suspend fun shouldSuggestNextEpisodeOnWatched(): Boolean =
        settingsStore.suggestNextEpisodeOnWatched.first()

    private suspend fun syncRemoteProgress(
        snapshot: PlayerProgressSnapshot,
        force: Boolean,
    ) {
        val videoId = snapshot.videoId
        if (videoId <= 0) return
        if (!isMeaningfulProgress(snapshot.positionMs, snapshot.durationMs)) {
            return
        }
        if (settingsStore.yaniUserId.first() <= 0) return

        val watchedEnough = isWatchedProgress(
            positionMs = snapshot.positionMs,
            durationMs = snapshot.durationMs,
        )
        if (watchedEnough && videoId in completedRemoteVideoIds) return
        if (videoId in syncingRemoteVideoIds) return

        val now = System.currentTimeMillis()
        val shouldForceCompletionSync =
            watchedEnough && videoId !in completionAttemptedVideoIds
        if (!force && !shouldForceCompletionSync && !isRemoteSyncDue(videoId, now)) return

        syncingRemoteVideoIds += videoId
        lastRemoteSyncAttemptAt[videoId] = now
        if (watchedEnough) completionAttemptedVideoIds += videoId
        val durationSeconds = (snapshot.durationMs / 1000L).toInt()
        val maxSecond = (durationSeconds - WATCH_END_TOLERANCE_SECONDS).coerceAtLeast(0)
        val timeSeconds = (snapshot.positionMs / 1000L).toInt().coerceIn(0, maxSecond)
        // Дельта — фиксированная копия ДО suspend-вызова; помечаем отправленной только при успехе.
        val synced = syncedSecondsByVideoId.getOrPut(videoId) { sortedSetOf() }
        val delta = (watchedSecondsByVideoId[videoId].orEmpty() - synced).sorted()
        runCatching {
            saveVideoWatchProgress(
                videoId = videoId,
                timeSeconds = timeSeconds,
                durationSeconds = durationSeconds,
                times = delta,
            )
        }.onSuccess {
            synced.addAll(delta)
            if (watchedEnough) completedRemoteVideoIds += videoId
        }.also {
            syncingRemoteVideoIds -= videoId
        }
    }

    private fun isRemoteSyncDue(videoId: Int, now: Long): Boolean {
        val lastAttempt = lastRemoteSyncAttemptAt[videoId] ?: return true
        return now - lastAttempt >= REMOTE_PROGRESS_SYNC_INTERVAL_MS
    }

}

/** Метаданные экрана, нужные для сохранения записи прогресса. */
internal data class PlayerProgressContext(
    val animeId: Int,
    val animeTitle: String,
    val posterUrl: String,
)
