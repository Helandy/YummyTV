package su.afk.yummy.tv.core.designsystem.presenter.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun MobileStateContent(
    isLoading: Boolean,
    error: String?,
    onRetry: (() -> Unit)? = null,
    empty: Boolean = false,
    emptyText: String = "Пока ничего нет",
    content: @Composable () -> Unit,
) {
    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        error != null -> MobileMessage(
            title = error,
            actionLabel = if (onRetry != null) "Повторить" else null,
            onAction = onRetry,
        )

        empty -> MobileMessage(title = emptyText)
        else -> content()
    }
}
