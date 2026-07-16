package su.afk.yummy.tv.core.designsystem.presenter.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.R
import su.afk.yummy.tv.core.designsystem.presenter.preview.MobileScreenPreview
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme

/** Ошибка подгрузки следующей страницы для мобильных списков. */
@Composable
fun MobileAppendError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

@MobileScreenPreview
@Composable
private fun MobileAppendErrorPreview() {
    ScreenPreviewTheme {
        MobileAppendError(
            message = "Не удалось загрузить следующую страницу",
            onRetry = {},
        )
    }
}
