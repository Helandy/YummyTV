package su.afk.yummy.tv.feature.player.behavior

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.domain.player.model.AllohaAudioTrack
import su.afk.yummy.tv.domain.player.model.AllohaSubtitleTrack
import su.afk.yummy.tv.feature.player.PlayerAnalytics
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.handler.PlayerAllohaRecoveryHandler
import su.afk.yummy.tv.feature.player.handler.PlayerAllohaSessionHandler
import su.afk.yummy.tv.feature.player.handler.PlayerAllohaTrackPreferenceHandler
import su.afk.yummy.tv.feature.player.handler.PlayerStreamLoadResult
import su.afk.yummy.tv.feature.player.host.PlayerSourceHost
import su.afk.yummy.tv.feature.player.host.PlayerStreamLoadRequest
import su.afk.yummy.tv.feature.player.utils.activeBalancerName
import su.afk.yummy.tv.feature.player.utils.activeDubbingName
import su.afk.yummy.tv.feature.player.utils.activeIframeUrl
import javax.inject.Inject

/**
 * Поведение Alloha: живая подписанная сессия, восстановление воспроизведения свежей сессией
 * и запомненный выбор аудиодорожки/субтитров.
 *
 * `session.selectAudioTrack()` затирает подписанный master сырым bnsi-URL, поэтому сохранённая
 * дорожка применяется только после того, как поток пошёл ([afterStreamApplied] идёт после
 * [onStreamActivated]), иначе часть CDN отвечает 403 `token_decrypt`. Сессия активируется только
 * при успешном резолве, а перед каждой новой попыткой восстановления закрывается, чтобы не
 * отдавать протухший токен. Порядок проверять на устройстве, а не упрощать по чтению.
 */
internal class AllohaSourceBehavior @Inject constructor(
    private val session: PlayerAllohaSessionHandler,
    private val recovery: PlayerAllohaRecoveryHandler,
    private val trackPreference: PlayerAllohaTrackPreferenceHandler,
    private val analytics: PlayerAnalytics,
) : PlayerSourceBehavior {

    private lateinit var host: PlayerSourceHost
    private var recoveryJob: Job? = null

    override val isRecovering: Boolean get() = recovery.isRecovering

    override fun attach(host: PlayerSourceHost) {
        this.host = host
    }

    override fun handles(state: PlayerState.State): Boolean =
        !state.isOfflinePlayback && state.isAllohaSource()

    override fun onPlaybackError(event: PlayerState.Event.PlaybackError): Boolean {
        if (recovery.isRecovering) {
            analytics.debugLog {
                "Ignoring duplicate Alloha playback error during fresh-session recovery " +
                    "positionMs=${event.positionMs.coerceAtLeast(0L)}"
            }
            return true
        }
        startRecovery(
            positionMs = event.positionMs,
            selectedQuality = host.state.selectedQuality,
            initialDelayMs = PLAYBACK_RECOVERY_DELAY_MS,
        )
        return true
    }

    override fun onRetryRequested(): Boolean {
        val state = host.state
        startRecovery(
            positionMs = state.playbackPositionMs.takeIf { it > 0L } ?: state.resumeFromMs,
            selectedQuality = state.selectedQuality,
            initialDelayMs = 0L,
        )
        return true
    }

    override fun onPlaybackRecovered() {
        analytics.debugLog {
            "Background Alloha playback recovery ready " +
                "positionMs=${host.state.playbackPositionMs.coerceAtLeast(0L)}"
        }
    }

    override fun onPlaybackPositionChanged(positionMs: Long) {
        if (recovery.isRecovering && positionMs > 0L) {
            recovery.positionMs = positionMs
        }
    }

    override fun keepsStreamWhileResolving(): Boolean =
        recovery.isRecovering && host.state.streamUrl != null

    override fun recoveryResumePositionMs(): Long? =
        recovery.positionMs.takeIf { recovery.isRecovering && it > 0L }

    override fun onStreamActivated(result: PlayerStreamLoadResult.State) {
        session.activate(result.allohaSession, host.scope)
        result.state.selectedQuality?.let { quality ->
            session.selectQuality(quality)
        }
    }

    override fun retriesFailedResolve(): Boolean {
        if (!recovery.isRecovering || !handles(host.state)) return false
        // No retry cap here, matching the reference implementation: a warm, pooled
        // WebView (see AllohaExtractor) makes each fresh-session attempt cheap
        // enough that retrying indefinitely is fine for transient CDN/token
        // rejections. A source that's permanently unavailable (not just
        // temporarily rejected) will keep retrying too - there's currently no way
        // to distinguish the two failure kinds here - so the user still needs to
        // navigate away or switch dubbing/balancer manually in that case.
        scheduleFreshAttempt(PLAYBACK_RECOVERY_DELAY_MS)
        return true
    }

    override fun onStreamResolved(result: PlayerStreamLoadResult.State, failed: Boolean): Boolean {
        val completed = recovery.isRecovering && handles(host.state)
        val attempts = recovery.complete()
        recoveryJob?.cancel()
        if (completed) {
            if (failed) {
                analytics.debugLog { "Background Alloha playback recovery failed attempts=$attempts" }
            } else {
                analytics.debugLog {
                    "Background Alloha playback recovery stream resolved " +
                        "attempts=$attempts " +
                        "positionMs=${result.state.resumeFromMs.coerceAtLeast(0L)}"
                }
            }
        }
        return completed
    }

    override suspend fun afterStreamApplied(result: PlayerStreamLoadResult.State) {
        if (host.state.isAllohaSource()) {
            restoreTrackPreference(
                audioTracks = result.state.allohaAudioTracks,
                subtitles = result.state.allohaSubtitles,
            )
        }
    }

    override fun reset() {
        recovery.reset()
        recoveryJob?.cancel()
    }

    override fun close(immediately: Boolean) {
        session.close(immediately)
    }

    /** Качество меняется внутри живой сессии; идущее восстановление подхватит его же. */
    fun onQualitySelected(quality: String) {
        if (recovery.isRecovering) {
            recovery.selectedQuality = quality
        }
        session.selectQuality(quality)
    }

    fun onAudioTrackSelected(audioId: String, positionMs: Long) {
        val position = positionMs.coerceAtLeast(0L)
        // Same live session, different stream URL - no re-extraction, like quality.
        val stream = session.selectAudioTrack(audioId) ?: return
        host.update {
            copy(
                selectedAllohaAudioId = stream.selectedAllohaAudioId,
                streamQualityMap = stream.qualities,
                selectedQuality = selectedQuality?.takeIf {
                    stream.qualities?.containsKey(it) == true
                },
                streamUrl = stream.url,
                resumeFromMs = position,
                playbackPositionMs = position,
            )
        }
        saveAudioPreference(audioId)
    }

    fun onSubtitleSelected(index: Int?) {
        val validIndex = index?.takeIf { it in host.state.allohaSubtitles.indices }
        host.update {
            copy(selectedAllohaSubtitleIndex = validIndex)
        }
        saveSubtitlePreference(validIndex)
    }

    private fun startRecovery(
        positionMs: Long,
        selectedQuality: String?,
        initialDelayMs: Long,
    ) {
        host.cancelStreamLoad()
        recoveryJob?.cancel()
        session.close()
        val resumePosition = positionMs.coerceAtLeast(0L)
        recovery.start(resumePosition, selectedQuality)
        host.changePlayerHint.cancel()
        host.update {
            copy(
                playerError = null,
                kodikBlockedError = null,
                resumeFromMs = resumePosition,
                playbackPositionMs = resumePosition,
                isPlaybackRecovering = true,
                showChangePlayerHint = false,
            )
        }
        // Если восстановление затянулось дольше [RECOVERY_HINT_DELAY_MS] - предлагаем юзеру
        // сменить плеер/озвучку, не дожидаясь исчерпания попыток.
        host.changePlayerHint.start(RECOVERY_HINT_DELAY_MS) { recovery.isRecovering }
        analytics.debugLog {
            "Starting fresh Alloha playback recovery positionMs=$resumePosition " +
                "quality=${selectedQuality ?: "auto"}"
        }
        scheduleFreshAttempt(initialDelayMs)
    }

    private fun scheduleFreshAttempt(delayMs: Long) {
        recoveryJob?.cancel()
        if (!recovery.canRetry()) {
            // Попытки исчерпаны: снимаем оверлей восстановления и показываем настоящую ошибку с
            // действиями, вместо того чтобы крутиться дальше без шанса на успех.
            // Пользователь увидит ошибку, а обычный путь PlaybackError сюда не доходит
            // (ошибку перехватило восстановление) — отправляем её явно.
            analytics.eventPlaybackError(
                state = host.state,
                message = "Alloha playback recovery giving up after " +
                    "${PlayerAllohaRecoveryHandler.MAX_ATTEMPTS} attempts",
                errorCode = null,
                errorType = ALLOHA_RECOVERY_EXHAUSTED_ERROR_TYPE,
                positionMs = host.state.playbackPositionMs,
                retryAttempts = PlayerAllohaRecoveryHandler.MAX_ATTEMPTS,
            )
            recovery.reset()
            host.changePlayerHint.cancel()
            host.update {
                copy(
                    isPlaybackRecovering = false,
                    showChangePlayerHint = true,
                    playerError = host.streamErrorMessage(),
                )
            }
            return
        }
        val iframeUrl = activeIframeUrl(host.state)
        val attempt = recovery.nextAttempt()
        recoveryJob = host.scope.launch {
            delay(delayMs)
            if (
                activeIframeUrl(host.state) == iframeUrl &&
                handles(host.state) &&
                recovery.isRecovering
            ) {
                session.close()
                analytics.debugLog {
                    "Opening fresh Alloha playback session " +
                        "attempt=$attempt/${PlayerAllohaRecoveryHandler.MAX_ATTEMPTS} " +
                        "positionMs=${recovery.positionMs}"
                }
                host.loadStream(
                    PlayerStreamLoadRequest(
                        refreshSourcesOnFailure = false,
                        forceFreshAllohaSession = true,
                        selectedQualityOverride = recovery.selectedQuality,
                    ),
                )
            }
        }
    }

    /**
     * Применяет ранее сохранённый выбор аудиодорожки/субтитров Alloha (по [PlayerAllohaTrackPreferenceHandler])
     * к только что распарсенным спискам, если он отличается от дефолта экстрактора.
     */
    private suspend fun restoreTrackPreference(
        audioTracks: List<AllohaAudioTrack>,
        subtitles: List<AllohaSubtitleTrack>,
    ) {
        if (audioTracks.isEmpty() && subtitles.isEmpty()) return
        val match = trackPreference.findMatch(
            animeId = host.state.animeId,
            dubbing = activeDubbingName(host.state),
            player = activeBalancerName(host.state),
            audioTracks = audioTracks,
            subtitles = subtitles,
        ) ?: return

        val audioId = match.audioId
        if (audioId != null && audioId != host.state.selectedAllohaAudioId) {
            val stream = session.selectAudioTrack(audioId)
            if (stream != null) {
                host.update {
                    copy(
                        selectedAllohaAudioId = stream.selectedAllohaAudioId,
                        streamQualityMap = stream.qualities,
                        selectedQuality = selectedQuality?.takeIf {
                            stream.qualities?.containsKey(it) == true
                        },
                        streamUrl = stream.url,
                    )
                }
            }
        }

        if (match.applySubtitleChange && match.subtitleIndex != host.state.selectedAllohaSubtitleIndex) {
            val index = match.subtitleIndex
            host.update {
                copy(selectedAllohaSubtitleIndex = index?.takeIf { it in subtitles.indices })
            }
        }
    }

    /** Запоминает выбранную пользователем аудиодорожку Alloha для текущей озвучки тайтла. */
    private fun saveAudioPreference(audioId: String) {
        val state = host.state
        val label = state.allohaAudioTracks.firstOrNull { it.id == audioId }?.label ?: return
        val animeId = state.animeId
        val dubbing = activeDubbingName(state)
        val player = activeBalancerName(state)
        host.scope.launch {
            trackPreference.saveAudioSelection(
                animeId = animeId,
                dubbing = dubbing,
                player = player,
                audioLabel = label,
            )
        }
    }

    /** Запоминает выбор субтитров Alloha (или их отключение) для текущей озвучки тайтла. */
    private fun saveSubtitlePreference(index: Int?) {
        val state = host.state
        val subtitle = index?.let { state.allohaSubtitles.getOrNull(it) }
        val animeId = state.animeId
        val dubbing = activeDubbingName(state)
        val player = activeBalancerName(state)
        host.scope.launch {
            trackPreference.saveSubtitleSelection(
                animeId = animeId,
                dubbing = dubbing,
                player = player,
                subtitleLanguage = subtitle?.language,
                subtitleLabel = subtitle?.label,
                subtitleOff = index == null,
            )
        }
    }

    private companion object {
        fun PlayerState.State.isAllohaSource(): Boolean =
            activeBalancerName(this).contains(ALLOHA_PLAYER_NAME, ignoreCase = true)

        private const val ALLOHA_PLAYER_NAME = "alloha"
        private const val ALLOHA_RECOVERY_EXHAUSTED_ERROR_TYPE = "alloha_recovery_exhausted"
        private const val PLAYBACK_RECOVERY_DELAY_MS = 1_000L
        private const val RECOVERY_HINT_DELAY_MS = 15_000L
    }
}
