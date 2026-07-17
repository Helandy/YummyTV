package su.afk.yummy.tv.feature.messages.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.messages.model.ChatMessage
import su.afk.yummy.tv.feature.messages.mobile.R
import su.afk.yummy.tv.feature.messages.mobile.utils.formatMessageDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatMessageMobileBubble(
    message: ChatMessage,
    isOwn: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onHistory: () -> Unit,
    onClaim: () -> Unit,
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box {
            Surface(
                color = if (isOwn) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { menuExpanded = true },
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 14.dp,
                        top = 10.dp,
                        end = 6.dp,
                        bottom = 6.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    message.reply?.let { reply ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = .5f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                reply.nickname?.let {
                                    Text(
                                        stringResource(R.string.messages_reply, it),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                Text(
                                    reply.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                )
                            }
                        }
                    }
                    Text(
                        text = if (message.isDeleted) stringResource(R.string.messages_deleted)
                        else message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = if (message.isDeleted) FontStyle.Italic else FontStyle.Normal,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = buildString {
                                if (message.dateSeconds > 0) append(
                                    message.dateSeconds.formatMessageDate(context)
                                )
                                if (message.isEdited) {
                                    if (isNotEmpty()) append(" · ")
                                    append(context.getString(R.string.messages_edited))
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.messages_actions),
                            )
                        }
                    }
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                if (isOwn && !message.isDeleted) {
                    MessageMenuItem(R.string.messages_edit, Icons.Filled.Edit) {
                        menuExpanded = false
                        onEdit()
                    }
                    MessageMenuItem(R.string.messages_delete, Icons.Filled.Delete) {
                        menuExpanded = false
                        onDelete()
                    }
                }
                if (isOwn && message.isDeleted) {
                    MessageMenuItem(R.string.messages_restore, Icons.Filled.Restore) {
                        menuExpanded = false
                        onRestore()
                    }
                }
                MessageMenuItem(R.string.messages_history, Icons.Filled.History) {
                    menuExpanded = false
                    onHistory()
                }
                if (!isOwn && !message.isDeleted) {
                    MessageMenuItem(R.string.messages_claim, Icons.Filled.Flag) {
                        menuExpanded = false
                        onClaim()
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageMenuItem(
    textRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(textRes)) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}
