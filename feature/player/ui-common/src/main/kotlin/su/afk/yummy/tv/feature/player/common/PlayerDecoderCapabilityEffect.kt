package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.feature.player.PlayerState

/**
 * Сторож «декодер не тянет формат». Если у устройства нет декодера под профиль/уровень выбранной видеодорожки (например,
 * HEVC Main10 на эмуляторе), ExoPlayer всё равно пытается играть: звук идёт, а видео не показывается, и плеер вечно
 * «буферизует» без единой ошибки. Когда выбранная дорожка выходит за возможности декодера и первый кадр не показан за
 * [FIRST_FRAME_GRACE_MS], шлём ту же ошибку декодера, что и сам плеер, — пользователь сразу видит причину.
 */
@Composable
fun PlayerDecoderCapabilityEffect(
    player: Player,
    onEvent: (PlayerState.Event) -> Unit,
) {
    val currentOnEvent by rememberUpdatedState(onEvent)
    val scope = rememberCoroutineScope()

    DisposableEffect(player) {
        var watchdog: Job? = null

        fun cancel() {
            watchdog?.cancel()
            watchdog = null
        }

        fun evaluate(tracks: Tracks) {
            cancel()
            val format = tracks.unsupportedSelectedVideoFormat() ?: return
            watchdog = scope.launch {
                delay(FIRST_FRAME_GRACE_MS)
                currentOnEvent(
                    PlayerState.Event.PlaybackError(
                        message = "Video format ${format.sampleMimeType} ${format.codecs.orEmpty()} " +
                            "is not supported by this device decoder",
                        errorCode = ERROR_CODE,
                        errorType = ERROR_TYPE,
                        positionMs = player.currentPosition.coerceAtLeast(0L),
                    ),
                )
            }
        }

        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) = evaluate(tracks)

            override fun onRenderedFirstFrame() = cancel()
        }
        player.addListener(listener)
        evaluate(player.currentTracks)
        onDispose {
            cancel()
            player.removeListener(listener)
        }
    }
}

/** Выбранная видеодорожка, которую не поддерживает ни один декодер устройства, иначе `null`. */
private fun Tracks.unsupportedSelectedVideoFormat(): Format? {
    for (group in groups) {
        if (group.type != C.TRACK_TYPE_VIDEO || !group.isSelected) continue
        for (index in 0 until group.length) {
            if (group.isTrackSelected(index) && !group.isTrackSupported(index, false)) {
                return group.getTrackFormat(index)
            }
        }
    }
    return null
}

private const val FIRST_FRAME_GRACE_MS = 3_000L
private const val ERROR_CODE = "ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES"
private const val ERROR_TYPE = "decoder_capability"
