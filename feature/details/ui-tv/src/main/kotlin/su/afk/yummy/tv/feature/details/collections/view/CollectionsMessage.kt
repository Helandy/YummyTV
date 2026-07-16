package su.afk.yummy.tv.feature.details.collections.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.feature.details.R

@Composable
internal fun CollectionsMessage(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvStateMessage(
        title = text,
        icon = Icons.Filled.Warning,
        retryLabel = stringResource(R.string.retry),
        onRetry = onRetry,
        fillMaxSize = false,
        modifier = modifier,
    )
}
