package su.afk.yummy.tv.feature.player.behavior

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import su.afk.yummy.tv.feature.player.PlayerAnalytics
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.handler.PlayerPlaybackRetryHandler
import su.afk.yummy.tv.feature.player.host.PlayerSourceHost
import su.afk.yummy.tv.feature.player.host.PlayerStreamLoadRequest
import su.afk.yummy.tv.feature.player.utils.activeIframeUrl
import javax.inject.Inject

/**
 * Поведение онлайн-источников без собственной сессии (Kodik, VK, CVH, Rutube, …).
 *
 * Тихий повтор воспроизведения: держим последний кадр + спиннер и молча перерезолвим источник.
 * До [PlayerPlaybackRetryHandler.MAX_ATTEMPTS] раз, дальше — оверлей ошибки.
 */
internal class DefaultSourceBehavior @Inject constructor(
    private val retry: PlayerPlaybackRetryHandler,
    private val analytics: PlayerAnalytics,
) : PlayerSourceBehavior {

    private lateinit var host: PlayerSourceHost
    private var retryJob: Job? = null

    /** Сколько тихих повторов потрачено в текущем сеансе — для аналитики финальной ошибки. */
    val retryAttempts: Int get() = retry.attempts

    override fun attach(host: PlayerSourceHost) {
        this.host = host
    }

    override fun handles(state: PlayerState.State): Boolean = !state.isOfflinePlayback

    override fun onPlaybackError(event: PlayerState.Event.PlaybackError): Boolean {
        if (!retry.canRetry()) return false
        scheduleRetryAttempt()
        return true
    }

    override fun onPlaybackRecovered() {
        analytics.debugLog {
            "Silent playback retry recovered " +
                "positionMs=${host.state.playbackPositionMs.coerceAtLeast(0L)}"
        }
    }

    override fun onPlaybackReady() {
        // Успешный старт - бюджет тихих повторов освобождается для нового сеанса.
        retry.reset()
    }

    override fun reset() {
        retry.reset()
        retryJob?.cancel()
    }

    private fun scheduleRetryAttempt() {
        retryJob?.cancel()
        val iframeUrl = activeIframeUrl(host.state)
        val attempt = retry.next()
        host.changePlayerHint.cancel()
        host.update {
            copy(
                playerError = null,
                isPlaybackRecovering = true,
                showChangePlayerHint = false,
            )
        }
        analytics.debugLog {
            "Silent playback retry attempt=$attempt/${PlayerPlaybackRetryHandler.MAX_ATTEMPTS}"
        }
        retryJob = host.scope.launch {
            // Re-resolve starts immediately (no artificial delay) so the visible stall is bounded
            // by the resolve+rebuild time only, not padded by a fixed wait before it even begins.
            if (
                activeIframeUrl(host.state) == iframeUrl &&
                !host.state.isOfflinePlayback
            ) {
                host.update { copy(retryKey = retryKey + 1) }
                host.closeSourceSessions()
                host.loadStream(
                    PlayerStreamLoadRequest(
                        refreshSourcesOnFailure = true,
                        selectedQualityOverride = host.state.selectedQuality,
                        forceRefresh = true,
                    ),
                )
            }
        }
    }
}
