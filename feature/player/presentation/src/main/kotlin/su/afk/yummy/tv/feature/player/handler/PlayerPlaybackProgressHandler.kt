package su.afk.yummy.tv.feature.player.handler

import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.feature.player.PlayerAnalytics
import su.afk.yummy.tv.feature.player.PlayerProgressSnapshot
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.utils.activeBalancerName
import su.afk.yummy.tv.feature.player.utils.activeDubbingName
import su.afk.yummy.tv.feature.player.utils.activeEpisode
import su.afk.yummy.tv.feature.player.utils.activeIframeUrl
import su.afk.yummy.tv.feature.player.utils.activeScreenshotUrl
import su.afk.yummy.tv.feature.player.utils.activeVideoId
import javax.inject.Inject

private data class PlayerCompletionAnalyticsKey(
    val animeId: Int,
    val videoId: Int,
    val episode: String,
    val iframeUrl: String,
)

/**
 * Координирует сохранение прогресса просмотра и аналитику завершения эпизода.
 *
 * Handler не владеет корутинами: он готовит запросы на сохранение и выполняет suspend-операции,
 * а ViewModel решает, когда запускать их в своем scope.
 */
internal class PlayerPlaybackProgressHandler @Inject constructor(
    private val progressHandler: PlayerProgressHandler,
    private val analytics: PlayerAnalytics,
) {
    private val completedAnalyticsSources = mutableSetOf<PlayerCompletionAnalyticsKey>()
    private val fullyCompletedAnalyticsSources = mutableSetOf<PlayerCompletionAnalyticsKey>()

    /** Создает запрос на обычное сохранение прогресса из события плеера. */
    fun progressSaveRequest(
        state: PlayerState.State,
        snapshot: PlayerProgressSnapshot,
    ): PlayerProgressSaveRequest =
        PlayerProgressSaveRequest(
            context = state.progressContext(),
            snapshot = snapshot,
        )

    /**
     * Создает запрос на сохранение текущего прогресса перед уходом с экрана.
     *
     * Возвращает `null`, если активный источник неполный и сохранять нечего.
     */
    fun currentProgressSaveRequest(state: PlayerState.State): PlayerProgressSaveRequest? =
        state.progressSnapshot(
            positionMs = state.playbackPositionMs,
            durationMs = state.playbackDurationMs,
        )?.let { snapshot ->
            PlayerProgressSaveRequest(
                context = state.progressContext(),
                snapshot = snapshot,
                forceRemoteSync = true,
            )
        }

    /**
     * Создает запрос на continue target для следующей серии.
     *
     * Для первой серии запрос не создается, чтобы не перетирать нормальную точку продолжения
     * стартовым эпизодом.
     */
    fun continueTargetRequest(state: PlayerState.State): PlayerContinueTargetRequest? {
        val snapshot = state.progressSnapshot(positionMs = 0L, durationMs = 0L) ?: return null
        if (snapshot.episode.isFirstEpisodeNumber()) return null
        return PlayerContinueTargetRequest(
            context = state.progressContext(),
            snapshot = snapshot,
        )
    }

    /**
     * Отправляет аналитику полного завершения эпизода один раз для активного источника.
     */
    fun reportEpisodeFullyCompleted(
        state: PlayerState.State,
        positionMs: Long,
        durationMs: Long,
    ) {
        val key = state.completionAnalyticsKey() ?: return
        if (!fullyCompletedAnalyticsSources.add(key)) return

        analytics.eventEpisodeFullyCompleted(
            state = state,
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = durationMs.coerceAtLeast(0L),
        )
    }

    /**
     * Отправляет аналитику просмотра и готовит запрос на сохранение, если эпизод досмотрен.
     *
     * Возвращает `null`, если прогресс еще не считается просмотренным или событие уже обработано.
     */
    fun watchedProgressRequest(
        state: PlayerState.State,
        positionMs: Long,
        durationMs: Long,
    ): PlayerProgressSaveRequest? {
        val position = positionMs.coerceAtLeast(0L)
        val duration = durationMs.coerceAtLeast(0L)
        if (!WatchProgressStore.isWatchedProgress(position, duration)) return null

        val key = state.completionAnalyticsKey() ?: return null
        if (!completedAnalyticsSources.add(key)) return null

        analytics.eventEpisodeCompleted(
            state = state,
            positionMs = position,
            durationMs = duration,
        )

        val snapshot = state.progressSnapshot(position, duration) ?: return null
        return PlayerProgressSaveRequest(
            context = state.progressContext(),
            snapshot = snapshot,
        )
    }

    /** Выполняет сохранение прогресса просмотра. */
    suspend fun saveProgress(request: PlayerProgressSaveRequest) {
        progressHandler.saveProgress(
            context = request.context,
            snapshot = request.snapshot,
            forceRemoteSync = request.forceRemoteSync,
        )
    }

    /** Выполняет сохранение точки продолжения просмотра. */
    suspend fun saveContinueTarget(request: PlayerContinueTargetRequest) {
        progressHandler.saveContinueTarget(
            context = request.context,
            snapshot = request.snapshot,
        )
    }

    /** Скрывает тайтл из Continue Watching, не удаляя историю просмотра серий. */
    suspend fun suppressContinueWatchingDisplay(state: PlayerState.State) {
        progressHandler.suppressContinueWatchingDisplay(state.progressContext())
    }

    private fun PlayerState.State.progressContext(): PlayerProgressContext =
        PlayerProgressContext(
            animeId = animeId,
            animeTitle = animeTitle,
            posterUrl = posterUrl,
        )

    private fun PlayerState.State.progressSnapshot(
        positionMs: Long,
        durationMs: Long,
    ): PlayerProgressSnapshot? {
        val episodeUrl = activeIframeUrl(this)
        val episode = activeEpisode(this)
        if (episodeUrl.isBlank() || episode.isBlank()) return null
        return PlayerProgressSnapshot(
            episode = episode,
            episodeUrl = episodeUrl,
            videoId = activeVideoId(this),
            playerName = activeBalancerName(this),
            dubbing = activeDubbingName(this),
            screenshotUrl = activeScreenshotUrl(this),
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    private fun PlayerState.State.completionAnalyticsKey(): PlayerCompletionAnalyticsKey? {
        val animeId = animeId.takeIf { it > 0 } ?: return null
        val videoId = activeVideoId(this)
        val episode = activeEpisode(this)
        val iframeUrl = activeIframeUrl(this)
        if (videoId <= 0 && episode.isBlank() && iframeUrl.isBlank()) return null
        return PlayerCompletionAnalyticsKey(
            animeId = animeId,
            videoId = videoId,
            episode = episode,
            iframeUrl = iframeUrl,
        )
    }

    private fun String.isFirstEpisodeNumber(): Boolean {
        val normalized = trim().replace(',', '.')
        val number = normalized.toDoubleOrNull()
            ?: Regex("""\d+(?:[.,]\d+)?""")
                .find(normalized)
                ?.value
                ?.replace(',', '.')
                ?.toDoubleOrNull()
        return number == 1.0
    }
}

/** Запрос на сохранение прогресса просмотра. */
internal data class PlayerProgressSaveRequest(
    val context: PlayerProgressContext,
    val snapshot: PlayerProgressSnapshot,
    val forceRemoteSync: Boolean = false,
)

/** Запрос на сохранение точки продолжения просмотра. */
internal data class PlayerContinueTargetRequest(
    val context: PlayerProgressContext,
    val snapshot: PlayerProgressSnapshot,
)
