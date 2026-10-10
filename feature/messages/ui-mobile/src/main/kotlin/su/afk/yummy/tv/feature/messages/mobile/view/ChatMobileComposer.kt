package su.afk.yummy.tv.feature.messages.mobile.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.feature.messages.presentation.R

@Composable
internal fun ChatMobileComposer(
    text: String,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    replyingToNickname: String? = null,
    replyingToText: String? = null,
    onCancelReply: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (replyingToNickname != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = 12.dp,
                        top = 6.dp,
                        end = 4.dp,
                        bottom = 6.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.messages_reply, replyingToNickname),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!replyingToText.isNullOrBlank()) {
                            Text(
                                text = replyingToText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(onClick = onCancelReply) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(CoreR.string.common_cancel),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                shape = MaterialTheme.shapes.large,
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                placeholder = { Text(stringResource(R.string.messages_composer_hint)) },
                modifier = Modifier.weight(1f),
                maxLines = 5,
            )
            IconButton(onClick = onSend, enabled = enabled && text.isNotBlank()) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.messages_send),
                )
            }
        }
    }
}
