package su.afk.yummy.tv.feature.player.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.feature.player.common.StepSeekDirection
import su.afk.yummy.tv.feature.player.model.MobileSeekDirection

internal val MobileSeekDirection.toastIcon: ImageVector
    get() = when (this) {
        MobileSeekDirection.Backward -> Icons.Filled.FastRewind
        MobileSeekDirection.Forward -> Icons.Filled.FastForward
    }

internal fun MobileSeekDirection.toStepSeekDirection(): StepSeekDirection =
    when (this) {
        MobileSeekDirection.Backward -> StepSeekDirection.Backward
        MobileSeekDirection.Forward -> StepSeekDirection.Forward
    }
