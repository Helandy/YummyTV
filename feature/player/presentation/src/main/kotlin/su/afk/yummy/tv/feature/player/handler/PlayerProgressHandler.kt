package su.afk.yummy.tv.feature.player.handler

import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.account.usecase.SaveVideoWatchProgressUseCase
import su.afk.yummy.tv.feature.player.PlayerProgressSnapshot
import javax.inject.Inject

private const val REMOTE_PROGRESS_SYNC_INTERVAL_MS = 10_000L

/** Persists local watch progress and silently mirrors remote playback progress. */
internal class PlayerProgressHandler @Inject constructor(
    private val watchProgressStore: WatchProgressStore,
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
            watchProgressStore.save(
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
        watchProgressStore.saveContinueTarget(
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

/** Screen-level metadata needed to store a progress entry. */
internal data class PlayerProgressContext(
    val animeId: Int,
    val animeTitle: String,
    val posterUrl: String,
)
