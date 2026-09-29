package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.media3.common.Player

/**
 * Применяет «продвинутую» громкость (внутренний уровень плеера 0..1). Когда она выключена, плеер
 * играет на 100%, а громкостью управляет система; сохранённый уровень при этом не сбрасывается.
 */
@Composable
fun PlayerVolumeEffect(
    player: Player,
    advancedVolumeEnabled: Boolean,
    volumeLevel: Float,
) {
    LaunchedEffect(player, advancedVolumeEnabled, volumeLevel) {
        player.volume = if (advancedVolumeEnabled) volumeLevel else 1f
    }
}
