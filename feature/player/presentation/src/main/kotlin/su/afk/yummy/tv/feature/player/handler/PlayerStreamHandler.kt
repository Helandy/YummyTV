package su.afk.yummy.tv.feature.player.handler

import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.player.isAllohaPlayerUrl
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.usecase.OpenAllohaStreamSessionUseCase
import su.afk.yummy.tv.domain.player.usecase.ResolvePlayerStreamUseCase
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.activeEpisode
import su.afk.yummy.tv.feature.player.utils.activeIframeUrl
import javax.inject.Inject

/** Resolves the active player iframe into a playable stream and presentation-ready stream errors. */
internal class PlayerStreamHandler @Inject constructor(
    private val watchProgressStore: WatchProgressStore,
    private val settingsStore: SettingsStore,
    private val resolvePlayerStream: ResolvePlayerStreamUseCase,
    private val openAllohaStreamSession: OpenAllohaStreamSessionUseCase,
    private val strings: StringProvider,
) {
    suspend fun resolve(
        state: PlayerState.State,
        pendingResumeMs: Long?,
        reuseAllohaPlaybackSession: Boolean = true,
        selectedQualityOverride: String? = null,
    ): PlayerStreamResult {
        val request = PlayerStreamRequest(
            iframeUrl = activeIframeUrl(state),
            autoQualityLabel = strings.get(R.string.player_quality_auto),
            sessionFallbackTtlSeconds = ALLOHA_PLAYBACK_FALLBACK_SESSION_TTL_SECONDS,
            reusePlaybackSession = reuseAllohaPlaybackSession,
        )
        val session = if (request.iframeUrl.isAllohaPlayerUrl()) {
            openAllohaStreamSession(request)
        } else null
        val resolved = session?.initialStream ?: resolvePlayerStream(request)
        return when (val result = resolved) {
            is PlayerStreamResolveResult.Stream -> {
                val resume = pendingResumeMs
                    ?: loadResumePosition(state.animeId, activeEpisode(state))
                    ?: 0L
                val selectedQuality = selectedQualityOverride
                    ?.takeIf { quality -> result.qualities?.containsKey(quality) == true }
                    ?: selectedQuality(result.qualities)
                if (selectedQuality != null) session?.selectQuality(selectedQuality)
                PlayerStreamResult.Stream(
                    url = result.url,
                    headers = result.headers,
                    qualities = result.qualities,
                    selectedQuality = selectedQuality,
                    resumeFromMs = resume,
                    consumedPendingResume = pendingResumeMs != null,
                    allohaSession = session,
                )
            }

            is PlayerStreamResolveResult.KodikBlocked -> {
                session?.close()
                PlayerStreamResult.KodikBlocked(message = result.toMessage())
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
        val entry = watchProgressStore.get(animeId, episode) ?: return null
        return entry.positionMs.takeIf { WatchProgressStore.isContinueWatchingEntry(entry) }
    }

    private suspend fun selectedQuality(
        qualities: LinkedHashMap<String, String>?,
    ): String? {
        val preferred = settingsStore.preferredVideoQuality.first()
        val preferredHeight = preferred.height ?: return null
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

    private fun String.qualityHeight(): Int? =
        Regex("""\d+""").find(this)?.value?.toIntOrNull()

    private fun PlayerStreamResolveResult.KodikBlocked.toMessage(): String =
        message
            ?: statusCode?.let { strings.get(R.string.player_server_error, it) }
            ?: strings.get(R.string.player_kodik_blocked)

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
