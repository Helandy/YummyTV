package su.afk.yummy.tv.feature.player.handler

import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.usecase.ResolvePlayerStreamUseCase
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.activeEpisode
import su.afk.yummy.tv.feature.player.utils.activeIframeUrl
import javax.inject.Inject

/** Resolves the active player iframe into a playable stream and presentation-ready stream errors. */
internal class PlayerStreamHandler @Inject constructor(
    private val watchProgressStore: WatchProgressStore,
    private val resolvePlayerStream: ResolvePlayerStreamUseCase,
    private val strings: StringProvider,
) {
    suspend fun resolve(
        state: PlayerState.State,
        pendingResumeMs: Long?,
    ): PlayerStreamResult {
        return when (val result = resolvePlayerStream(
            PlayerStreamRequest(
                iframeUrl = activeIframeUrl(state),
                autoQualityLabel = strings.get(R.string.player_quality_auto),
            )
        )) {
            is PlayerStreamResolveResult.Stream -> {
                val resume = pendingResumeMs
                    ?: loadResumePosition(state.animeId, activeEpisode(state))
                    ?: 0L
                PlayerStreamResult.Stream(
                    url = result.url,
                    headers = result.headers,
                    qualities = result.qualities,
                    resumeFromMs = resume,
                    consumedPendingResume = pendingResumeMs != null,
                )
            }

            is PlayerStreamResolveResult.KodikBlocked ->
                PlayerStreamResult.KodikBlocked(message = result.toMessage())

            PlayerStreamResolveResult.Failed ->
                PlayerStreamResult.PlayerError(
                    message = strings.get(R.string.player_stream_error),
                    reason = PlayerStreamResult.REASON_FAILED,
                )

            PlayerStreamResolveResult.Unsupported ->
                PlayerStreamResult.PlayerError(
                    message = strings.get(R.string.player_unsupported),
                    reason = PlayerStreamResult.REASON_UNSUPPORTED,
                )
        }
    }

    fun playbackErrorMessage(message: String): String {
        val detail = message.trim().takeIf { it.isNotBlank() }
        return buildString {
            append(strings.get(R.string.player_stream_error))
            if (detail != null) {
                append("\n")
                append(detail)
            }
        }
    }

    private suspend fun loadResumePosition(animeId: Int, episode: String): Long? {
        val entry = watchProgressStore.get(animeId, episode) ?: return null
        return entry.positionMs.takeIf { WatchProgressStore.isContinueWatchingEntry(entry) }
    }

    private fun PlayerStreamResolveResult.KodikBlocked.toMessage(): String =
        message
            ?: statusCode?.let { strings.get(R.string.player_server_error, it) }
            ?: strings.get(R.string.player_kodik_blocked)
}

/** Result of resolving the currently selected player source. */
internal sealed interface PlayerStreamResult {
    data class Stream(
        val url: String,
        val headers: Map<String, String>,
        val qualities: LinkedHashMap<String, String>?,
        val resumeFromMs: Long,
        val consumedPendingResume: Boolean,
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
    }
}
