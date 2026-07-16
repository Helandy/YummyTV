package su.afk.yummy.tv.core.designsystem.presenter.tv

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen

@Composable
fun TvStateContent(
    isLoading: Boolean,
    error: String?,
    empty: Boolean,
    emptyText: String,
    modifier: Modifier = Modifier,
    emptyDescription: String? = null,
    emptyIcon: ImageVector = Icons.Filled.Info,
    onRetry: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    when {
        isLoading -> TvLoadingScreen(modifier)

        error != null -> TvStateMessage(
            title = error,
            icon = Icons.Filled.Warning,
            onRetry = onRetry,
            modifier = modifier,
        )

        empty -> TvStateMessage(
            title = emptyText,
            description = emptyDescription,
            icon = emptyIcon,
            modifier = modifier,
        )

        else -> content()
    }
}
