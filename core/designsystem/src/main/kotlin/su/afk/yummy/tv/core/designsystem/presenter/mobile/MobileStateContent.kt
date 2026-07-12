package su.afk.yummy.tv.core.designsystem.presenter.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.R

@Composable
fun MobileStateContent(
    isLoading: Boolean,
    error: String?,
    onRetry: (() -> Unit)? = null,
    empty: Boolean = false,
    emptyText: String = stringResource(R.string.empty_screen),
    emptyDescription: String? = null,
    emptyIcon: ImageVector = Icons.Filled.Info,
    content: @Composable () -> Unit,
) {
    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        error != null -> MobileMessage(
            title = error,
            actionLabel = if (onRetry != null) stringResource(R.string.retry) else null,
            onAction = onRetry,
        )

        empty -> MobileMessage(title = emptyText, description = emptyDescription, icon = emptyIcon)
        else -> content()
    }
}
