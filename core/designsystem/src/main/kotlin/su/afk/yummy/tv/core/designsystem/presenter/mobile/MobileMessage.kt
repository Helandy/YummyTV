package su.afk.yummy.tv.core.designsystem.presenter.mobile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.designsystem.presenter.components.StateMessage

@Composable
fun MobileMessage(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector = Icons.Filled.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    fillMaxSize: Boolean = true,
) {
    StateMessage(
        title = title,
        modifier = modifier,
        description = description,
        icon = icon,
        actionLabel = actionLabel,
        onAction = onAction,
        fillMaxSize = fillMaxSize,
    )
}
