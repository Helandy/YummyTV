package su.afk.yummy.tv.feature.player.handler

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.anime.isContinueWatchingProgress
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.utils.player.isAllohaPlayerUrl
import su.afk.yummy.tv.domain.player.model.AllohaAudioTrack
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.model.AllohaSubtitleTrack
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.usecase.GetWatchProgressUseCase
import su.afk.yummy.tv.domain.player.usecase.OpenAllohaStreamSessionUseCase
import su.afk.yummy.tv.domain.player.usecase.ResolvePlayerStreamUseCase
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.activeEpisode
import su.afk.yummy.tv.feature.player.utils.activeIframeUrl
import su.afk.yummy.tv.feature.player.utils.qualityHeight
import su.afk.yummy.tv.feature.player.utils.toMessage
import javax.inject.Inject

/** Resolves the active player iframe into a playable stream and presentation-ready stream errors. */
internal class PlayerStreamHandler @Inject constructor(
    private val getWatchProgress: GetWatchProgressUseCase,
    private val settingsStore: PlayerSettingsStore,
    private val resolvePlayerStream: ResolvePlayerStreamUseCase,
    private val openAllohaStreamSession: OpenAllohaStreamSessionUseCase,
    private val strings: StringProvider,
) {
    suspend fun resolve(
        state: PlayerState.State,
        pendingResumeMs: Long?,
        reuseAllohaPlaybackSession: Boolean = true,
        selectedQualityOverride: String? = null,
        forceRefresh: Boolean = false,
    ): PlayerStreamResult = coroutineScope {
        val request = PlayerStreamRequest(
            iframeUrl = activeIframeUrl(state),
            autoQualityLabel = strings.get(R.string.player_quality_auto),
            sessionFallbackTtlSeconds = ALLOHA_PLAYBACK_FALLBACK_SESSION_TTL_SECONDS,
            reusePlaybackSession = reuseAllohaPlaybackSession,
            forceRefresh = forceRefresh,
        )
        // Started before the extraction is awaited: neither depends on it, and on the Alloha path
        // openAllohaStreamSession() drives a WebView for seconds, during which a DataStore read and
        // a Room read would otherwise just sit in the queue behind it.
        //
        // The saved dubbing is deliberately NOT applied here: selectAudioTrack() resets the live
        // session's master URL to the bnsi one parsed out of the source list, and that URL carries
        // a path token the CDN answers with 403 token_decrypt once the session is live. It may only
        // run after the correctly-signed master is in play, which is why it stays in the ViewModel.
        val preferredHeightAsync = async { settingsStore.preferredVideoQuality.first().height }
        val resumeAsync = async {
            pendingResumeMs ?: loadResumePosition(state.animeId, activeEpisode(state))
        }

        val session = if (request.iframeUrl.isAllohaPlayerUrl()) {
            openAllohaStreamSession(request)
        } else null
        // Свежая сессия (без переиспользования) ещё никому не принадлежит — её регистрирует activate
        // во ViewModel. Отмена или сбой на await ниже иначе оставили бы открытыми WebView и прокси.
        // Переиспользованную сессию не трогаем: она живая и принадлежит менеджеру сессий.
        try {
            val resolved = session?.initialStream ?: resolvePlayerStream(request)
            when (val result = resolved) {
                is PlayerStreamResolveResult.Stream -> {
                    val selectedQuality = selectedQualityOverride
                        ?.takeIf { quality -> result.qualities?.containsKey(quality) == true }
                        ?: selectedQuality(result.qualities, preferredHeightAsync.await())
                    if (selectedQuality != null) session?.selectQuality(selectedQuality)
                    PlayerStreamResult.Stream(
                        url = result.url,
                        headers = result.headers,
                        qualities = result.qualities,
                        selectedQuality = selectedQuality,
                        resumeFromMs = resumeAsync.await() ?: 0L,
                        consumedPendingResume = pendingResumeMs != null,
                        allohaSession = session,
                        allohaAudioTracks = result.allohaAudioTracks,
                        selectedAllohaAudioId = result.selectedAllohaAudioId,
                        allohaSubtitles = result.allohaSubtitles,
                    )
                }

                is PlayerStreamResolveResult.KodikBlocked -> {
                    session?.close()
                    PlayerStreamResult.KodikBlocked(message = result.toMessage(strings))
                }

                is PlayerStreamResolveResult.Unavailable -> {
                    session?.close()
                    PlayerStreamResult.PlayerError(
                        message = result.message ?: strings.get(R.string.player_dubbing_unavailable),
                        reason = PlayerStreamResult.REASON_UNAVAILABLE,
                    )
                }

                PlayerStreamResolveResult.Failed -> {
                    session?.close()
                    PlayerStreamResult.PlayerError(
                        message = strings.get(R.string.player_stream_error),
                        reason = PlayerStreamResult.REASON_FAILED,
                    )
                }

                PlayerStreamResolveResult.Unsupported -> {
                    session?.close()
                    PlayerStreamResult.PlayerError(
                        message = strings.get(R.string.player_unsupported),
                        reason = PlayerStreamResult.REASON_UNSUPPORTED,
                    )
                }
            }
        } catch (e: Throwable) {
            if (!request.reusePlaybackSession) session?.close()
            throw e
        }
    }

    fun playbackErrorMessage(message: String, errorCode: String? = null): String {
        val detail = message.trim().takeIf { it.isNotBlank() }
        val code = errorCode?.trim()?.takeIf { it.isNotBlank() && it != detail }
        return buildString {
            append(strings.get(R.string.player_stream_error))
            if (detail != null) {
                append("\n")
                append(detail)
            }
            if (code != null) {
                append("\n")
                append(code)
            }
        }
    }

    private suspend fun loadResumePosition(animeId: Int, episode: String): Long? {
        val progress = getWatchProgress(animeId, episode) ?: return null
        return progress.positionMs.takeIf { progress.isContinueWatchingProgress() }
    }

    private fun selectedQuality(
        qualities: LinkedHashMap<String, String>?,
        preferredHeight: Int?,
    ): String? {
        if (preferredHeight == null) return null
        val available = qualities
            ?.keys
            ?.mapNotNull { label -> label.qualityHeight()?.let { height -> label to height } }
            .orEmpty()
        if (available.isEmpty()) return null
        return available.firstOrNull { (_, height) -> height == preferredHeight }?.first
            ?: available
                .filter { (_, height) -> height < preferredHeight }
                .maxByOrNull { (_, height) -> height }
                ?.first
            ?: available.maxByOrNull { (_, height) -> height }?.first
    }

    private companion object {
        const val ALLOHA_PLAYBACK_FALLBACK_SESSION_TTL_SECONDS = 120
    }
}

/** Result of resolving the currently selected player source. */
internal sealed interface PlayerStreamResult {
    data class Stream(
        val url: String,
        val headers: Map<String, String>,
        val qualities: LinkedHashMap<String, String>?,
        val selectedQuality: String?,
        val resumeFromMs: Long,
        val consumedPendingResume: Boolean,
        val allohaSession: AllohaStreamSession?,
        val allohaAudioTracks: List<AllohaAudioTrack> = emptyList(),
        val selectedAllohaAudioId: String? = null,
        val allohaSubtitles: List<AllohaSubtitleTrack> = emptyList(),
    ) : PlayerStreamResult

    data class KodikBlocked(val message: String) : PlayerStreamResult
    data class PlayerError(
        val message: String,
        val reason: String,
    ) : PlayerStreamResult

    companion object {
        const val REASON_EXCEPTION = "exception"
        const val REASON_FAILED = "failed"
        const val REASON_KODIK_BLOCKED = "kodik_blocked"
        const val REASON_UNSUPPORTED = "unsupported"
        const val REASON_UNAVAILABLE = "unavailable"
    }
}
