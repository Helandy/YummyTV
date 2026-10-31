package su.afk.yummy.tv.feature.player.mobile.utils

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.feature.player.presentation.R

@get:StringRes
internal val PlayerOrientationMode.labelRes: Int
    get() = when (this) {
        PlayerOrientationMode.SYSTEM -> R.string.player_mobile_orientation_system
        PlayerOrientationMode.LEFT -> R.string.player_mobile_orientation_left
        PlayerOrientationMode.RIGHT -> R.string.player_mobile_orientation_right
    }

@get:StringRes
internal val PlayerOrientationMode.hintRes: Int
    get() = when (this) {
        PlayerOrientationMode.SYSTEM -> R.string.player_mobile_orientation_system_hint
        PlayerOrientationMode.LEFT -> R.string.player_mobile_orientation_left_hint
        PlayerOrientationMode.RIGHT -> R.string.player_mobile_orientation_right_hint
    }

internal val PlayerOrientationMode.icon: ImageVector
    get() = when (this) {
        PlayerOrientationMode.SYSTEM -> Icons.Rounded.ScreenRotation
        PlayerOrientationMode.RIGHT -> Icons.Rounded.RotateRight
        PlayerOrientationMode.LEFT -> Icons.Rounded.RotateLeft
    }
