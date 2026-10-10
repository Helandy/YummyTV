package su.afk.yummy.tv.feature.player.mobile.utils

import androidx.annotation.StringRes
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
