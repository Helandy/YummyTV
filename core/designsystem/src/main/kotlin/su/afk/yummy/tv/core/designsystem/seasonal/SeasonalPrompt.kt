package su.afk.yummy.tv.core.designsystem.seasonal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import su.afk.yummy.tv.core.designsystem.R
import su.afk.yummy.tv.core.designsystem.dialog.ConfirmDialogRequest
import su.afk.yummy.tv.core.designsystem.dialog.TvConfirmDialog
import su.afk.yummy.tv.core.model.seasonal.SeasonalTheme

/**
 * Один раз сообщает, что включилось праздничное оформление [theme], и даёт сразу отключить его.
 * Показывается, пока [theme] задана и [ready] (нет полноэкранных экранов и обязательного
 * обновления). Закрытие без выбора оставляет оформление; [onAnswer] получает `true` при отключении.
 */
@Composable
fun SeasonalPrompt(
    theme: SeasonalTheme?,
    ready: Boolean,
    isTelevision: Boolean,
    onAnswer: (disable: Boolean) -> Unit,
) {
    if (theme == null || theme == SeasonalTheme.NONE || !ready) return
    val title = stringResource(R.string.seasonal_prompt_title)
    val message = stringResource(
        if (theme == SeasonalTheme.HALLOWEEN) R.string.seasonal_prompt_halloween else R.string.seasonal_prompt_new_year,
    )
    val keep = stringResource(R.string.seasonal_prompt_keep)
    val disable = stringResource(R.string.seasonal_prompt_disable)
    if (isTelevision) {
        val request = remember(title, message, keep, disable) {
            ConfirmDialogRequest(title = title, message = message, confirmText = keep, dismissText = disable)
        }
        TvConfirmDialog(
            request = request,
            onAnswer = { kept -> onAnswer(!kept) },
            onDismiss = { onAnswer(false) },
        )
    } else {
        Dialog(onDismissRequest = { onAnswer(false) }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = { onAnswer(false) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(keep)
                    }
                    OutlinedButton(onClick = { onAnswer(true) }, modifier = Modifier.fillMaxWidth()) {
                        Text(disable)
                    }
                }
            }
        }
    }
}
