package su.afk.yummy.tv.feature.player.mobile.utils

import android.content.Context
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.feature.player.mobile.model.MobileVerticalGestureZone
import kotlin.math.roundToInt

internal const val MOBILE_PLAYER_MIN_BRIGHTNESS = 0.01f

internal fun readSystemBrightnessFraction(context: Context): Float = try {
    Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        .coerceIn(0, 255) / 255f
} catch (_: Settings.SettingNotFoundException) {
    0.5f
}

internal fun Float.toGesturePercentText(): String =
    "${(coerceIn(0f, 1f) * 100f).roundToInt()}%"

internal val MobileVerticalGestureZone.gestureIcon: ImageVector
    get() = when (this) {
        MobileVerticalGestureZone.Brightness -> Icons.Filled.BrightnessMedium
        MobileVerticalGestureZone.Volume -> Icons.Filled.VolumeUp
    }
