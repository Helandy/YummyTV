package su.afk.yummy.tv.feature.player.common

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Управление системной громкостью (STREAM_MUSIC): плеер не держит свой уровень,
 * а меняет тот же, что и аппаратные кнопки.
 */
@Stable
class PlayerSystemVolumeController(private val audioManager: AudioManager?) {

    private val maxIndex: Int
        get() = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 1

    private val currentIndex: Int
        get() = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0

    fun currentFraction(): Float = (currentIndex.toFloat() / maxIndex).coerceIn(0f, 1f)

    fun setFraction(fraction: Float): Float =
        applyIndex((fraction.coerceIn(0f, 1f) * maxIndex).roundToInt())

    /**
     * Сдвиг громкости на долю шкалы. Если после округления индекс не изменился
     * (шаг мельче деления системной шкалы) — двигаем на одно деление.
     */
    fun stepBy(delta: Float): Float {
        val max = maxIndex
        val current = currentIndex
        val target = ((current.toFloat() / max + delta).coerceIn(0f, 1f) * max).roundToInt()
        val index = if (target == current && delta != 0f) current + delta.sign.toInt() else target
        return applyIndex(index)
    }

    private fun applyIndex(index: Int): Float {
        val max = maxIndex
        val safeIndex = index.coerceIn(0, max)
        // DND может запрещать смену громкости — тогда просто оставляем текущий уровень
        runCatching {
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, safeIndex, 0)
        }
        return currentFraction()
    }
}

@Composable
fun rememberPlayerSystemVolumeController(): PlayerSystemVolumeController {
    val context: Context = LocalContext.current
    return remember(context) {
        PlayerSystemVolumeController(context.getSystemService<AudioManager>())
    }
}
