package su.afk.yummy.tv.feature.player.handler

import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.watchprogress.RemoteContinueWatchingStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.account.usecase.SaveVideoWatchProgressUseCase
import su.afk.yummy.tv.feature.player.PlayerProgressSnapshot
import javax.inject.Inject

private const val REMOTE_PROGRESS_SYNC_INTERVAL_MS = 10_000L

/** Сохраняет локальный прогресс просмотра и тихо синхронизирует его с сервером. */
internal class PlayerProgressHandler @Inject constructor(
    private val watchProgressStore: WatchProgressStore,
    private val remoteContinueWatchingStore: RemoteContinueWatchingStore,
    private val settingsStore: SettingsStore,
    private val saveVideoWatchProgress: SaveVideoWatchProgressUseCase,
) {
    private val completedRemoteVideoIds = mutableSetOf<Int>()
    private val completionAttemptedVideoIds = mutableSetOf<Int>()
    private val syncingRemoteVideoIds = mutableSetOf<Int>()
    private val lastRemoteSyncAttemptAt = mutableMapOf<Int, Long>()

    suspend fun saveProgress(
        context: PlayerProgressContext,
        snapshot: PlayerProgressSnapshot,
        forceRemoteSync: Boolean = false,
    ) {
        if (snapshot.durationMs <= 0) return
        val savedSnapshot = snapshot.withFullTimingIfWatched()

        if (context.animeId > 0 && savedSnapshot.episode.isNotBlank()) {
            val updatedAt = localActivityUpdatedAt(context.animeId, savedSnapshot.episode)
            watchProgressStore.save(
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
        watchProgressStore.saveContinueTarget(
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

        val accountKey = continueWatchingAccountKey()
        val language = settingsStore.yaniContentLanguage.first().apiCode
        val remoteUpdatedAt = remoteContinueWatchingStore.latestUpdatedAt(
            accountKey = accountKey,
            language = language,
            animeId = animeId,
        )
        val existingUpdatedAt = episode
            .takeIf { it.isNotBlank() }
            ?.let { watchProgressStore.get(animeId, it)?.updatedAt }
            ?: 0L

        return maxOf(now, remoteUpdatedAt + 1L, existingUpdatedAt + 1L)
    }

    private suspend fun continueWatchingAccountKey(): String {
        val userId = settingsStore.yaniUserId.first()
        return if (userId > 0) "user:$userId" else "anon"
    }

    suspend fun suppressContinueWatchingDisplay(context: PlayerProgressContext) {
        val now = System.currentTimeMillis()
        val accountKey = continueWatchingAccountKey()
        val language = settingsStore.yaniContentLanguage.first().apiCode
        val remoteUpdatedAt = remoteContinueWatchingStore.latestUpdatedAt(
            accountKey = accountKey,
            language = language,
            animeId = context.animeId,
        )
        watchProgressStore.suppressContinueWatchingDisplay(
            animeId = context.animeId,
            suppressedAt = maxOf(now, remoteUpdatedAt + 1L),
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
        if (!WatchProgressStore.isMeaningfulProgress(snapshot.positionMs, snapshot.durationMs)) {
            return
        }
        if (settingsStore.yaniUserId.first() <= 0) return

        val watchedEnough = WatchProgressStore.isWatchedProgress(
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
        runCatching {
            saveVideoWatchProgress(
                videoId = videoId,
                timeSeconds = (snapshot.positionMs / 1000L).toInt(),
                durationSeconds = (snapshot.durationMs / 1000L).toInt(),
            )
        }.onSuccess {
            if (watchedEnough) completedRemoteVideoIds += videoId
        }.also {
            syncingRemoteVideoIds -= videoId
        }
    }

    private fun isRemoteSyncDue(videoId: Int, now: Long): Boolean {
        val lastAttempt = lastRemoteSyncAttemptAt[videoId] ?: return true
        return now - lastAttempt >= REMOTE_PROGRESS_SYNC_INTERVAL_MS
    }

    private fun PlayerProgressSnapshot.withFullTimingIfWatched(): PlayerProgressSnapshot =
        if (WatchProgressStore.isWatchedProgress(positionMs, durationMs)) {
            copy(positionMs = durationMs)
        } else {
            this
        }
}

/** Метаданные экрана, нужные для сохранения записи прогресса. */
internal data class PlayerProgressContext(
    val animeId: Int,
    val animeTitle: String,
    val posterUrl: String,
)
