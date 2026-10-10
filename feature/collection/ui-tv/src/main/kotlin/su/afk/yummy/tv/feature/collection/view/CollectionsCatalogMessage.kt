package su.afk.yummy.tv.feature.collection.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.tv.TvStateMessage
import su.afk.yummy.tv.feature.collection.presentation.R

@Composable
internal fun CollectionsCatalogMessage(
    message: String,
    onRetry: () -> Unit,
    retryFocusRequester: FocusRequester,
) {
    TvStateMessage(
        title = message,
        icon = Icons.Filled.Warning,
        retryLabel = stringResource(CoreR.string.common_retry),
        onRetry = onRetry,
        retryFocusRequester = retryFocusRequester,
    )
}
