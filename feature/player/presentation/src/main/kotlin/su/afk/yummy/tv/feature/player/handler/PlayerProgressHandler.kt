package su.afk.yummy.tv.feature.player.handler

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import su.afk.yummy.tv.core.model.anime.isMeaningfulProgress
import su.afk.yummy.tv.core.model.anime.isWatchedProgress
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.account.usecase.SaveVideoWatchProgressUseCase
import su.afk.yummy.tv.domain.player.usecase.SaveContinueTargetUseCase
import su.afk.yummy.tv.domain.player.usecase.SaveWatchProgressUseCase
import su.afk.yummy.tv.domain.player.usecase.SuppressContinueWatchingDisplayUseCase
import su.afk.yummy.tv.feature.player.model.PlayerProgressSnapshot
import su.afk.yummy.tv.feature.player.utils.withFullTimingIfWatched
import javax.inject.Inject

private const val REMOTE_PROGRESS_SYNC_INTERVAL_MS = 10_000L

/** Хвост эпизода, который на сервер не отправляем как позицию/секунду (как в веб-клиенте). */
private const val WATCH_END_TOLERANCE_SECONDS = 10

/**
 * Сохраняет локальный прогресс просмотра и тихо синхронизирует его с сервером.
 *
 * Один экземпляр на ViewModel: секунды копит ViewModel, а финальную отправку при уходе делает
 * `PlayerNavigationDelegate` — без общего скоупа у него был свой пустой экземпляр и последние
 * секунды перед выходом не уходили на сервер.
 */
@ViewModelScoped
internal class PlayerProgressHandler @Inject constructor(
    private val saveWatchProgress: SaveWatchProgressUseCase,
    private val saveContinueTargetUseCase: SaveContinueTargetUseCase,
    private val suppressContinueWatchingDisplayUseCase: SuppressContinueWatchingDisplayUseCase,
    private val settingsStore: SettingsStore,
    private val saveVideoWatchProgress: SaveVideoWatchProgressUseCase,
) {
    /**
     * Сериализует отправку на сервер: [saveProgress] зовут и с Main, и из app-scope (уход из плеера),
     * поэтому состояние синхронизации ниже трогается только под этим замком.
     */
    private val syncMutex = Mutex()
    private val completedRemoteVideoIds = mutableSetOf<Int>()
    private val completionAttemptedVideoIds = mutableSetOf<Int>()
    private val lastRemoteSyncAttemptAt = mutableMapOf<Int, Long>()

    /** Замок коротких секций над [watchedSecondsByVideoId]: его пишет тик плеера, читает синхронизация. */
    private val watchedLock = Any()

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
        synchronized(watchedLock) {
            watchedSecondsByVideoId.getOrPut(videoId) { sortedSetOf() }.add(second)
        }
    }

    suspend fun saveProgress(
        context: PlayerProgressContext,
        snapshot: PlayerProgressSnapshot,
        forceRemoteSync: Boolean = false,
    ) {
        if (snapshot.durationMs <= 0) return
        val savedSnapshot = snapshot.withFullTimingIfWatched()

        if (context.animeId > 0 && savedSnapshot.episode.isNotBlank()) {
            saveWatchProgress(
                animeId = context.animeId,
                episode = savedSnapshot.episode,
                videoId = savedSnapshot.videoId,
                episodeUrl = savedSnapshot.episodeUrl,
                positionMs = savedSnapshot.positionMs,
                durationMs = savedSnapshot.durationMs,
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
        saveContinueTargetUseCase(
            animeId = context.animeId,
            episode = snapshot.episode,
            videoId = snapshot.videoId,
            episodeUrl = snapshot.episodeUrl,
            animeTitle = context.animeTitle,
            posterUrl = context.posterUrl,
            playerName = snapshot.playerName,
            dubbing = snapshot.dubbing,
            screenshotUrl = snapshot.screenshotUrl,
        )
    }

    suspend fun suppressContinueWatchingDisplay(context: PlayerProgressContext) {
        suppressContinueWatchingDisplayUseCase(context.animeId)
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

        // Плановая отправка не встаёт в очередь за идущей: следующий тик пришлёт дельту. Финальная
        // (уход с экрана) дожидается, иначе накопленные секунды потерялись бы вместе с VM.
        if (force) syncMutex.lock() else if (!syncMutex.tryLock()) return
        try {
            syncRemoteProgressLocked(snapshot, force)
        } finally {
            syncMutex.unlock()
        }
    }

    private suspend fun syncRemoteProgressLocked(
        snapshot: PlayerProgressSnapshot,
        force: Boolean,
    ) {
        val videoId = snapshot.videoId
        val watchedEnough = isWatchedProgress(
            positionMs = snapshot.positionMs,
            durationMs = snapshot.durationMs,
        )
        if (watchedEnough && videoId in completedRemoteVideoIds) return

        val now = System.currentTimeMillis()
        val shouldForceCompletionSync =
            watchedEnough && videoId !in completionAttemptedVideoIds
        if (!force && !shouldForceCompletionSync && !isRemoteSyncDue(videoId, now)) return

        lastRemoteSyncAttemptAt[videoId] = now
        if (watchedEnough) completionAttemptedVideoIds += videoId
        val durationSeconds = (snapshot.durationMs / 1000L).toInt()
        val maxSecond = (durationSeconds - WATCH_END_TOLERANCE_SECONDS).coerceAtLeast(0)
        val timeSeconds = (snapshot.positionMs / 1000L).toInt().coerceIn(0, maxSecond)
        // Дельта — фиксированная копия ДО suspend-вызова; помечаем отправленной только при успехе.
        val synced = syncedSecondsByVideoId.getOrPut(videoId) { sortedSetOf() }
        val delta = synchronized(watchedLock) {
            watchedSecondsByVideoId[videoId].orEmpty() - synced
        }.sorted()
        val accepted = runSuspendCatching {
            saveVideoWatchProgress(
                videoId = videoId,
                timeSeconds = timeSeconds,
                durationSeconds = durationSeconds,
                times = delta,
            )
        }.getOrDefault(false)
        if (accepted) {
            synced.addAll(delta)
            if (watchedEnough) completedRemoteVideoIds += videoId
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
