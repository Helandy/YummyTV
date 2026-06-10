package su.afk.yummy.tv.feature.player.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.feature.player.model.MobileSeekDirection

internal const val MOBILE_PLAYER_STEP_SEEK_RESET_MS = 1_500L
internal val MOBILE_PLAYER_STEP_SEEK_OFFSETS_MS = longArrayOf(5_000L, 10_000L, 15_000L)

internal val MobileSeekDirection.toastIcon: ImageVector
    get() = when (this) {
        MobileSeekDirection.Backward -> Icons.Filled.FastRewind
        MobileSeekDirection.Forward -> Icons.Filled.FastForward
    }

internal fun Long.formatSignedSeconds(): String {
    val seconds = this / 1_000L
    val prefix = if (seconds > 0) "+" else ""
    return "${prefix}${seconds}s"
}
