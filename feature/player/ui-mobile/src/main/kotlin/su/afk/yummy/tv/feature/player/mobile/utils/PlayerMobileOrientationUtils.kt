package su.afk.yummy.tv.feature.player.mobile.utils

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.StayCurrentLandscape
import androidx.compose.material.icons.rounded.StayCurrentPortrait
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.feature.player.presentation.R

@get:StringRes
internal val PlayerOrientationMode.labelRes: Int
    get() = when (this) {
        PlayerOrientationMode.SYSTEM -> R.string.player_mobile_orientation_system
        PlayerOrientationMode.SENSOR_LANDSCAPE -> R.string.player_mobile_orientation_sensor_landscape
        PlayerOrientationMode.PORTRAIT -> R.string.player_mobile_orientation_portrait
    }

@get:StringRes
internal val PlayerOrientationMode.hintRes: Int
    get() = when (this) {
        PlayerOrientationMode.SYSTEM -> R.string.player_mobile_orientation_system_hint
        PlayerOrientationMode.SENSOR_LANDSCAPE -> R.string.player_mobile_orientation_sensor_landscape_hint
        PlayerOrientationMode.PORTRAIT -> R.string.player_mobile_orientation_portrait_hint
    }

internal val PlayerOrientationMode.icon: ImageVector
    get() = when (this) {
        PlayerOrientationMode.SYSTEM -> Icons.Rounded.ScreenRotation
        PlayerOrientationMode.SENSOR_LANDSCAPE -> Icons.Rounded.StayCurrentLandscape
        PlayerOrientationMode.PORTRAIT -> Icons.Rounded.StayCurrentPortrait
    }
