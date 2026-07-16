package su.afk.yummy.tv.core.designsystem.presenter.tv

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.R
import su.afk.yummy.tv.core.designsystem.presenter.components.StateMessage
import su.afk.yummy.tv.core.designsystem.presenter.focus.TvRetryButton
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.designsystem.presenter.preview.TvScreenPreview

/**
 * Центрированное состояние ошибки/пустоты для TV: иконка + заголовок + описание
 * + фокусируемая кнопка «Повторить» (TvRetryButton).
 */
@Composable
fun TvStateMessage(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Info,
    description: String? = null,
    retryLabel: String = stringResource(R.string.retry),
    onRetry: (() -> Unit)? = null,
    retryFocusRequester: FocusRequester? = null,
    fillMaxSize: Boolean = true,
) {
    StateMessage(
        title = title,
        modifier = modifier,
        icon = icon,
        iconSize = 56.dp,
        description = description,
        fillMaxSize = fillMaxSize,
        action = onRetry?.let {
            {
                TvRetryButton(
                    text = retryLabel,
                    modifier = if (retryFocusRequester != null) {
                        Modifier.focusRequester(retryFocusRequester)
                    } else {
                        Modifier
                    },
                    onClick = it,
                )
            }
        },
    )
}

@TvScreenPreview
@Composable
private fun TvStateMessageErrorPreview() {
    ScreenPreviewTheme {
        TvStateMessage(
            title = "Не удалось загрузить",
            description = "Проверьте подключение к интернету",
            icon = Icons.Filled.Warning,
            onRetry = {},
        )
    }
}

@TvScreenPreview
@Composable
private fun TvStateMessageEmptyPreview() {
    ScreenPreviewTheme {
        TvStateMessage(
            title = "Здесь пока пусто",
        )
    }
}
