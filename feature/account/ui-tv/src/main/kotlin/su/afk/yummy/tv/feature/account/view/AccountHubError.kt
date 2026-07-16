@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage

/** Компактная центрированная ошибка хаба аккаунта с кнопкой «Повторить». */
@Composable
internal fun AccountHubError(
    error: String?,
    onRetry: (() -> Unit)? = null,
) {
    if (error == null) return
    TvStateMessage(
        title = error,
        icon = Icons.Filled.Warning,
        onRetry = onRetry,
        fillMaxSize = false,
    )
}
