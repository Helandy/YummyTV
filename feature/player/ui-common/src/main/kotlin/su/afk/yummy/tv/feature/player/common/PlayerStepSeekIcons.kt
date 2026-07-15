package su.afk.yummy.tv.feature.player.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.ui.graphics.vector.ImageVector

val StepSeekDirection.toastIcon: ImageVector
    get() = when (this) {
        StepSeekDirection.Backward -> Icons.Filled.FastRewind
        StepSeekDirection.Forward -> Icons.Filled.FastForward
    }
