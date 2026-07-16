package su.afk.yummy.tv.core.designsystem.presenter.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.R
import su.afk.yummy.tv.core.designsystem.presenter.focus.TvRetryButton
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.designsystem.presenter.preview.TvScreenPreview

/** Ошибка подгрузки следующей страницы для TV-списков — зеркало TvLoadingFooter. */
@Composable
fun TvAppendErrorFooter(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(end = 16.dp),
        )
        TvRetryButton(
            text = stringResource(R.string.retry),
            onClick = onRetry,
        )
    }
}

@TvScreenPreview
@Composable
private fun TvAppendErrorFooterPreview() {
    ScreenPreviewTheme {
        TvAppendErrorFooter(
            message = "Не удалось загрузить следующую страницу",
            onRetry = {},
        )
    }
}
