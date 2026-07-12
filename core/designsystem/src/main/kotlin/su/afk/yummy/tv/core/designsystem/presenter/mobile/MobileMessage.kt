package su.afk.yummy.tv.core.designsystem.presenter.mobile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.designsystem.presenter.components.StateMessage

@Composable
fun MobileMessage(
    title: String,
    description: String? = null,
    icon: ImageVector = Icons.Filled.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StateMessage(
        title = title,
        description = description,
        icon = icon,
        actionLabel = actionLabel,
        onAction = onAction
    )
}
