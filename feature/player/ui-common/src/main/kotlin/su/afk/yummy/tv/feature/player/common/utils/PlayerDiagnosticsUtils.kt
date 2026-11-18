package su.afk.yummy.tv.feature.player.common.utils

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.media3.common.C
import androidx.media3.common.Player
import su.afk.yummy.tv.feature.player.common.model.PlayerDiagnosticsInfo
import java.util.Locale

private const val BYTES_IN_MB = 1024L * 1024L

/** Собирает снимок диагностики по плееру и системе. Вызывать с main-потока. */
fun Player.buildDiagnosticsInfo(
    context: Context,
    droppedFrames: Int,
    bandwidthBps: Long,
): PlayerDiagnosticsInfo {
    val memory = ActivityManager.MemoryInfo().also {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(it)
    }
    val videoFormat = currentTracks.groups
        .firstOrNull { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }
        ?.let { group -> (0 until group.length).firstOrNull { group.isTrackSelected(it) }?.let(group::getTrackFormat) }
    return PlayerDiagnosticsInfo(
        playbackState = playbackState,
        playWhenReady = playWhenReady,
        bufferedAheadMs = (bufferedPosition - currentPosition).coerceAtLeast(0L),
        totalBufferedMs = totalBufferedDuration,
        videoWidth = videoSize.width.takeIf { it > 0 } ?: videoFormat?.width?.takeIf { it > 0 } ?: 0,
        videoHeight = videoSize.height.takeIf { it > 0 } ?: videoFormat?.height?.takeIf { it > 0 } ?: 0,
        videoBitrate = videoFormat?.bitrate?.takeIf { it > 0 } ?: 0,
        videoCodec = videoFormat?.sampleMimeType,
        droppedFrames = droppedFrames,
        bandwidthBps = bandwidthBps,
        freeRamMb = memory.availMem / BYTES_IN_MB,
        totalRamMb = memory.totalMem / BYTES_IN_MB,
        network = context.networkKind(),
    )
}

private fun Context.networkKind(): String {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "?"
    val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return "none"
    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
        else -> "other"
    }
}

fun playbackStateName(state: Int): String = when (state) {
    Player.STATE_IDLE -> "IDLE"
    Player.STATE_BUFFERING -> "BUFFERING"
    Player.STATE_READY -> "READY"
    Player.STATE_ENDED -> "ENDED"
    else -> "?"
}

fun Long.toSecondsText(): String = String.format(Locale.US, "%.1f", this / 1000f)

fun Long.toMbpsText(): String =
    if (this <= 0L) "–" else String.format(Locale.US, "%.1f", this / 1_000_000f)

fun Int.toMbpsText(): String = toLong().toMbpsText()
