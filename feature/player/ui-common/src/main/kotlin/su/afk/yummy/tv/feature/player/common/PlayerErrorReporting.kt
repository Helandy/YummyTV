package su.afk.yummy.tv.feature.player.common

import android.util.Log
import androidx.media3.common.PlaybackException
import su.afk.yummy.tv.feature.player.PlayerState

const val PLAYBACK_LOG_TAG = "PlayerPlayback"

fun logPlaybackError(platformLabel: String, positionMs: Long, error: PlaybackException) {
    Log.e(
        PLAYBACK_LOG_TAG,
        "$platformLabel playback error positionMs=$positionMs code=${error.errorCodeName} " +
                "causes=${error.diagnosticCauseChain()}",
    )
}

fun PlaybackException.toPlaybackErrorEvent(positionMs: Long): PlayerState.Event.PlaybackError =
    PlayerState.Event.PlaybackError(
        message = localizedMessage
            ?: message
            ?: errorCodeName,
        errorCode = errorCodeName.takeIf { it.isNotBlank() },
        errorType = analyticsType(),
        positionMs = positionMs,
    )
