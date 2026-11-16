@file:JvmName("TvRecommendationActionsDialogKt")

package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.focus.TvRetryButton
import su.afk.yummy.tv.core.designsystem.focus.consumeLongPressRelease
import su.afk.yummy.tv.feature.home.presentation.R
import su.afk.yummy.tv.core.designsystem.focus.tvBackKeyForwarding

/** Подтверждение скрытия тайтла из блока рекомендаций на главной. */
@Composable
internal fun TvRecommendationActionsDialog(
    title: String,
    onHide: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dismissFocusRequester = remember { FocusRequester() }
    // По умолчанию фокус на «Отмена» — скрытие не должно срабатывать случайным нажатием OK.
    LaunchedEffect(Unit) {
        runCatching { dismissFocusRequester.requestFocus() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.tvBackKeyForwarding()
                .widthIn(max = 560.dp)
                .consumeLongPressRelease(),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_tv_recommendation_hide_title, title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.home_tv_recommendation_hide_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Обе кнопки в одном стиле: сфокусированная заливается primary, остальные —
                // прозрачные с рамкой, иначе на ТВ непонятно, что сейчас выбрано.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvRetryButton(
                        text = stringResource(CoreR.string.common_cancel),
                        onClick = onDismiss,
                        modifier = Modifier.focusRequester(dismissFocusRequester),
                    )
                    TvRetryButton(
                        text = stringResource(R.string.home_tv_recommendation_hide),
                        onClick = onHide,
                    )
                }
            }
        }
    }
}
