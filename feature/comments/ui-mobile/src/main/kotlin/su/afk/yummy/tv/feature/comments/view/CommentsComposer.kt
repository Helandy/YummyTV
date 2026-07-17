package su.afk.yummy.tv.feature.comments.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.feature.comments.CommentsState
import su.afk.yummy.tv.feature.comments.mobile.R

@Composable
internal fun CommentsComposer(
    isSignedIn: Boolean,
    text: String,
    mode: CommentsState.ComposerMode,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    Column {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        )
        Surface(
            tonalElevation = 1.dp,
            shadowElevation = 0.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            if (!isSignedIn) {
                Text(
                    text = stringResource(R.string.comments_sign_in_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
                return@Surface
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ComposerModeLabel(
                        mode = mode,
                        modifier = Modifier.weight(1f),
                    )
                    if (mode !is CommentsState.ComposerMode.New) {
                        TextButton(
                            onClick = onCancel,
                            enabled = enabled,
                        ) {
                            Text(stringResource(R.string.comments_cancel))
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = onTextChange,
                        enabled = enabled,
                        minLines = 1,
                        maxLines = 4,
                        placeholder = { Text(stringResource(R.string.comments_input_hint)) },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onSubmit,
                        enabled = enabled && text.isNotBlank(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.comments_send),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposerModeLabel(
    mode: CommentsState.ComposerMode,
    modifier: Modifier = Modifier,
) {
    when (mode) {
        CommentsState.ComposerMode.New -> Text(
            text = stringResource(R.string.comments_new_comment),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )

        is CommentsState.ComposerMode.Reply -> Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = mode.replyToAvatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            )
            Box(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.comments_reply_to, mode.replyToName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is CommentsState.ComposerMode.Edit -> Text(
            text = stringResource(R.string.comments_editing),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
}
