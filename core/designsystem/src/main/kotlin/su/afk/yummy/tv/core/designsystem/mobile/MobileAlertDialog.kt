package su.afk.yummy.tv.core.designsystem.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator

/**
 * Мобильный диалог с кнопками друг под другом на всю ширину: основное действие
 * заливкой, второстепенное ([dismissText]) обводкой. Подписи кнопок не ужимаются
 * в половину ширины и не переносятся, поэтому диалог не ломается на узком экране и
 * крупном шрифте. Содержимое ([content]) прокручивается, если не помещается по высоте;
 * [scrollableContent] = false нужен, когда внутри уже есть своя прокручиваемая область.
 */
@Composable
fun MobileAlertDialog(
    onDismissRequest: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    dismissText: String? = null,
    onDismiss: () -> Unit = onDismissRequest,
    properties: DialogProperties = DialogProperties(),
    confirmEnabled: Boolean = true,
    confirmLoading: Boolean = false,
    destructive: Boolean = false,
    scrollableContent: Boolean = true,
    dismissEnabled: Boolean = true,
    content: (@Composable () -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (icon != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) { icon() }
                }
                if (title != null) {
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.headlineSmall,
                    ) { title() }
                }
                if (content != null) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .then(if (scrollableContent) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                    ) {
                        CompositionLocalProvider(
                            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                        ) { content() }
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onConfirm,
                        enabled = confirmEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (destructive) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                    ) {
                        if (confirmLoading) {
                            YummyLoadingIndicator(
                                modifier = Modifier.size(18.dp),
                                color = LocalContentColor.current,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(confirmText)
                        }
                    }
                    if (dismissText != null) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = dismissEnabled,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(dismissText) }
                    }
                }
            }
        }
    }
}
