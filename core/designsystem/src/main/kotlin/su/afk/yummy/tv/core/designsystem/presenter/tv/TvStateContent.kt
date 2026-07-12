package su.afk.yummy.tv.core.designsystem.presenter.tv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.designsystem.presenter.components.StateMessage

@Composable
fun TvStateContent(
    isLoading: Boolean,
    error: String?,
    empty: Boolean,
    emptyText: String,
    modifier: Modifier = Modifier,
    emptyDescription: String? = null,
    emptyIcon: ImageVector = Icons.Filled.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    when {
        isLoading -> Box(
            modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        error != null -> StateMessage(
            title = error,
            actionLabel = actionLabel,
            onAction = onAction,
            modifier = modifier
        )

        empty -> StateMessage(
            title = emptyText,
            description = emptyDescription,
            icon = emptyIcon,
            modifier = modifier
        )

        else -> content()
    }
}
