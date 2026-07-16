package su.afk.yummy.tv.feature.details.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.feature.details.R

@Composable
internal fun DetailsError(
    message: String,
    onRetry: () -> Unit,
) {
    TvStateMessage(
        title = stringResource(R.string.details_error_title),
        description = message,
        icon = Icons.Filled.Warning,
        retryLabel = stringResource(R.string.retry),
        onRetry = onRetry,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    )
}
