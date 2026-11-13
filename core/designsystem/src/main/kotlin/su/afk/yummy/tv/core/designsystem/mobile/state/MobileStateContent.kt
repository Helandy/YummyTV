package su.afk.yummy.tv.core.designsystem.mobile.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.R
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator
import su.afk.yummy.tv.core.designsystem.motion.rememberStateChangeFade
import su.afk.yummy.tv.core.designsystem.motion.stateChangeFade

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
    val kind = when {
        isLoading -> MobileStateKind.Loading
        error != null -> MobileStateKind.Error
        empty -> MobileStateKind.Empty
        else -> MobileStateKind.Content
    }
    val fade = rememberStateChangeFade(kind)
    Box(Modifier.fillMaxSize().stateChangeFade(fade)) {
        when (kind) {
            MobileStateKind.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                YummyLoadingIndicator()
            }

            MobileStateKind.Error -> MobileMessage(
                title = error.orEmpty(),
                actionLabel = if (onRetry != null) stringResource(R.string.common_retry) else null,
                onAction = onRetry,
            )

            MobileStateKind.Empty ->
                MobileMessage(title = emptyText, description = emptyDescription, icon = emptyIcon)

            MobileStateKind.Content -> content()
        }
    }
}

private enum class MobileStateKind { Loading, Error, Empty, Content }
