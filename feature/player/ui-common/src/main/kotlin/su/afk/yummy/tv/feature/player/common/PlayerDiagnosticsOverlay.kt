package su.afk.yummy.tv.feature.player.common

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import su.afk.yummy.tv.feature.player.common.model.PlayerDiagnosticsInfo
import su.afk.yummy.tv.feature.player.common.utils.buildDiagnosticsInfo
import su.afk.yummy.tv.feature.player.common.utils.playbackStateName
import su.afk.yummy.tv.feature.player.common.utils.toMbpsText
import su.afk.yummy.tv.feature.player.common.utils.toSecondsText
import su.afk.yummy.tv.feature.player.presentation.R

private const val TICK_MS = 1000L

/**
 * Диагностика поверх видео при включённой записи логов: буфер, видео, декодер и система.
 * Не перехватывает ввод и фокус. Данные опрашиваются раз в секунду, пока оверлей в композиции.
 */
@Composable
fun PlayerDiagnosticsOverlay(
    player: Player,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 10.sp,
) {
    val context = LocalContext.current
    val signals = rememberPlayerQualitySignals()
    var dropped by remember { mutableIntStateOf(0) }
    var bandwidth by remember { mutableLongStateOf(signals.lastBandwidthBps) }
    var info by remember { mutableStateOf<PlayerDiagnosticsInfo?>(null) }

    LaunchedEffect(signals) {
        signals.events.collect { signal ->
            when (signal) {
                is PlayerQualitySignals.Signal.DroppedFrames -> dropped += signal.count
                is PlayerQualitySignals.Signal.Bandwidth -> bandwidth = signal.bitsPerSecond
            }
        }
    }
    LaunchedEffect(player) {
        while (true) {
            info = player.buildDiagnosticsInfo(context, dropped, bandwidth)
            delay(TICK_MS)
        }
    }

    val data = info ?: return
    val lines = listOf(
        stringResource(
            R.string.player_diag_buffer,
            data.bufferedAheadMs.toSecondsText(),
            data.totalBufferedMs.toSecondsText(),
            playbackStateName(data.playbackState),
            data.playWhenReady.toString(),
        ),
        stringResource(
            R.string.player_diag_video,
            data.videoWidth,
            data.videoHeight,
            data.videoBitrate.toMbpsText(),
            data.videoCodec.orEmpty(),
            data.droppedFrames,
            data.bandwidthBps.toMbpsText(),
        ),
        stringResource(
            R.string.player_diag_system,
            "${Build.MANUFACTURER} ${Build.MODEL}",
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT,
            data.freeRamMb,
            data.totalRamMb,
            data.network,
        ),
    )
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        lines.forEach { line ->
            Text(
                text = line,
                color = Color.White,
                fontSize = fontSize,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
