package su.afk.yummy.tv.feature.player.behavior

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import su.afk.yummy.tv.feature.player.PlayerAnalytics
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.handler.PlayerPlaybackRetryHandler
import su.afk.yummy.tv.feature.player.handler.PlayerStreamLoadResult
import su.afk.yummy.tv.feature.player.host.PlayerSourceHost
import su.afk.yummy.tv.feature.player.host.PlayerStreamLoadRequest
import su.afk.yummy.tv.feature.player.utils.activeIframeUrl
import javax.inject.Inject

/**
 * Поведение онлайн-источников без собственной сессии (Kodik, VK, CVH, Rutube, …).
 *
 * Тихий повтор воспроизведения: держим плеер и последний кадр на экране (без оверлея
 * «Получаем поток») и молча перерезолвим источник. Новый поток подменяет старый с той же позиции.
 * До [PlayerPlaybackRetryHandler.MAX_ATTEMPTS] раз, дальше — оверлей ошибки.
 */
internal class DefaultSourceBehavior @Inject constructor(
    private val retry: PlayerPlaybackRetryHandler,
    private val analytics: PlayerAnalytics,
) : PlayerSourceBehavior {

    private lateinit var host: PlayerSourceHost
    private var retryJob: Job? = null

    /** Идёт тихий перерезолв: старый поток остаётся на экране, пока не придёт новый. */
    private var recovering = false

    override val isRecovering: Boolean get() = recovering

    /** Сколько тихих повторов потрачено в текущем сеансе — для аналитики финальной ошибки. */
    override val retryAttempts: Int get() = retry.attempts

    override fun attach(host: PlayerSourceHost) {
        this.host = host
    }

    override fun handles(state: PlayerState.State): Boolean = !state.isOfflinePlayback

    override fun onPlaybackError(event: PlayerState.Event.PlaybackError): Boolean {
        // Отголосок упавшего потока, пока резолвится новый: попытка уже потрачена.
        if (recovering) return true
        if (!retry.canRetry()) return false
        scheduleRetryAttempt()
        return true
    }

    override fun onPlaybackStalled(): Boolean {
        if (recovering) return true
        if (!retry.canRetry()) return false
        analytics.debugLog {
            "Playback stalled positionMs=${host.state.playbackPositionMs.coerceAtLeast(0L)}"
        }
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

    override fun keepsStreamWhileResolving(): Boolean = recovering && host.state.streamUrl != null

    override fun retriesFailedResolve(): Boolean {
        if (!recovering || !retry.canRetry()) return false
        scheduleRetryAttempt()
        return true
    }

    override fun onStreamResolved(result: PlayerStreamLoadResult.State, failed: Boolean): Boolean {
        val completed = recovering
        recovering = false
        return completed
    }

    override fun reset() {
        recovering = false
        retry.reset()
        retryJob?.cancel()
    }

    private fun scheduleRetryAttempt() {
        retryJob?.cancel()
        val iframeUrl = activeIframeUrl(host.state)
        val attempt = retry.next()
        recovering = true
        host.update {
            copy(
                playerError = null,
                isPlaybackRecovering = true,
                showChangePlayerHint = false,
            )
        }
        // Затянувшийся тихий ретрай — предлагаем сменить плеер/озвучку, не дожидаясь исчерпания попыток.
        host.changePlayerHint.start(RECOVERY_HINT_DELAY_MS) { recovering }
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
                // retryKey не трогаем: старый URL мёртв, а ViewModel сам пересоберёт MediaItem
                // после резолва (completedRecovery в applyResolvedStream).
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

    private companion object {
        private const val RECOVERY_HINT_DELAY_MS = 10_000L
    }
}
