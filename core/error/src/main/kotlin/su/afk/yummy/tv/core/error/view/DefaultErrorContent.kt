package su.afk.yummy.tv.core.error.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.error.R
import su.afk.yummy.tv.core.model.ErrorItem

@Composable
fun DefaultErrorContent(
    errorItem: ErrorItem,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    var showDetails by remember { mutableStateOf(false) }

    val copyText = remember(errorItem) { errorItem.toClipboardText() }

    val hasDetails = remember(errorItem) {
        errorItem.code != null ||
                errorItem.method != null ||
                errorItem.url != null ||
                errorItem.requestId != null ||
                (errorItem.body?.isNotBlank() == true) ||
                (errorItem.cause?.isNotBlank() == true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = errorItem.title.ifBlank { stringResource(R.string.err_title_generic) },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = errorItem.message,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (hasDetails) {
                    OutlinedButton(
                        onClick = { showDetails = !showDetails },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (showDetails) stringResource(R.string.hide_details)
                            else stringResource(R.string.details)
                        )
                    }
                }

                DefaultErrorRetryButton(
                    text = stringResource(R.string.retry),
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                )
            }

            /** ДЕТАЛИ */
            AnimatedVisibility(visible = showDetails && hasDetails) {
                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    errorItem.code?.let {
                        Text(
                            stringResource(
                                R.string.error_http_code,
                                it.toString()
                            )
                        )
                    }
                    errorItem.method?.let { Text(stringResource(R.string.error_method, it)) }
                    errorItem.url?.let { Text(stringResource(R.string.error_url, it)) }
                    errorItem.requestId?.let { Text(stringResource(R.string.error_request_id, it)) }

                    errorItem.cause?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(text = it, style = MaterialTheme.typography.bodySmall)
                    }

                    errorItem.body?.takeIf { it.isNotBlank() }?.let { body ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.error_body, body),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    /** Copy показываем ТОЛЬКО когда раскрыты детали */
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { context.copyToClipboard(copyText) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(stringResource(R.string.copy))
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultErrorRetryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (focused) colorScheme.primary else Color.Transparent
    val contentColor = if (focused) colorScheme.onPrimary else colorScheme.onSurface
    val borderColor = if (focused) colorScheme.primary else colorScheme.outline.copy(alpha = 0.72f)

    Button(
        modifier = modifier,
        onClick = onClick,
        shape = shape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        border = BorderStroke(
            width = if (focused) 2.dp else 1.dp,
            color = borderColor,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(text = text)
    }
}

private fun ErrorItem.toClipboardText(): String {
    val lines = buildList {
        add("Title: $title")
        add("Message: $message")
        code?.let { add("HTTP: $it") }
        method?.let { add("Method: $it") }
        url?.let { add("URL: $it") }
        requestId?.let { add("RequestId: $it") }
        cause?.let { add("Cause: $it") }
        body?.takeIf { it.isNotBlank() }?.let {
            add("")
            add("Body:")
            add(it)
        }
    }
    return lines.joinToString("\n")
}

private fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("error", text))
}
