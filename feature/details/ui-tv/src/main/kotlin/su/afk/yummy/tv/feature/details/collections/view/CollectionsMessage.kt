package su.afk.yummy.tv.feature.details.collections.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.focus.TvRetryButton
import su.afk.yummy.tv.feature.details.R

@Composable
internal fun CollectionsMessage(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = text, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(12.dp))
        TvRetryButton(
            text = stringResource(R.string.retry),
            onClick = onRetry,
        )
    }
}
