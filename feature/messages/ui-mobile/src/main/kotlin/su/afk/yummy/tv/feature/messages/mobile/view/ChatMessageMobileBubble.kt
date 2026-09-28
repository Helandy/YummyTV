package su.afk.yummy.tv.feature.messages.mobile.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import su.afk.yummy.tv.domain.messages.model.ChatMessage
import su.afk.yummy.tv.feature.messages.mobile.R
import su.afk.yummy.tv.feature.messages.mobile.utils.formatMessageDate
import kotlin.math.roundToInt

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
    showAuthor: Boolean = false,
    onReply: () -> Unit = {},
    onReplyClick: (Int) -> Unit = {},
    onAuthorClick: () -> Unit = {},
    isHighlighted: Boolean = false,
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    val showAvatar = showAuthor && !isOwn
    val bubbleShape = RoundedCornerShape(18.dp)

    val density = LocalDensity.current
    val thresholdPx = with(density) { 56.dp.toPx() }
    val maxDragPx = with(density) { 72.dp.toPx() }
    val offsetX = remember(message.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentOnReply by rememberUpdatedState(onReply)
    val swipeProgress = (-offsetX.value / thresholdPx).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(message.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val target = (offsetX.value + dragAmount).coerceIn(-maxDragPx, 0f)
                        scope.launch { offsetX.snapTo(target) }
                    },
                    onDragEnd = {
                        if (offsetX.value <= -thresholdPx) currentOnReply()
                        scope.launch { offsetX.animateTo(0f) }
                    },
                    onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
                )
            },
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .alpha(swipeProgress),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) },
            contentAlignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                if (showAvatar) {
                    AsyncImage(
                        model = message.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onAuthorClick)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Spacer(Modifier.size(8.dp))
                }
                Box {
                    val bubbleColor by animateColorAsState(
                        targetValue = when {
                            isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
                            isOwn -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        label = "bubbleHighlight",
                    )
                    Surface(
                        color = bubbleColor,
                        shape = bubbleShape,
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(bubbleShape)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { menuExpanded = true },
                            ),
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                start = 14.dp,
                                top = 8.dp,
                                end = 14.dp,
                                bottom = 6.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (showAvatar && message.nickname.isNotBlank()) {
                                Text(
                                    text = message.nickname,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    modifier = Modifier.clickable(onClick = onAuthorClick),
                                )
                            }
                            message.reply?.let { reply ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = .5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable(enabled = reply.messageId != null) {
                                        reply.messageId?.let(onReplyClick)
                                    },
                                ) {
                                    Column(Modifier.padding(8.dp)) {
                                        reply.nickname?.let {
                                            Text(
                                                stringResource(R.string.messages_reply, it),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        Text(
                                            reply.text.lineSequence().firstOrNull().orEmpty(),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
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
                            Row(
                                modifier = Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                if (message.isEdited) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = stringResource(R.string.messages_edited),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                if (message.dateSeconds > 0) {
                                    Text(
                                        text = message.dateSeconds.formatMessageDate(context),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // Якорь нулевой ширины у правого края строки: меню прижимается к правой стороне диалога.
        Box(
            modifier = Modifier
                .matchParentSize()
                .wrapContentSize(Alignment.BottomEnd),
        ) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                offset = DpOffset(0.dp, 4.dp),
            ) {
                if (!message.isDeleted) {
                    MessageMenuItem(
                        R.string.messages_reply_action,
                        Icons.AutoMirrored.Filled.Reply
                    ) {
                        menuExpanded = false
                        onReply()
                    }
                }
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
                if (message.isEdited) {
                    MessageMenuItem(R.string.messages_history, Icons.Filled.History) {
                        menuExpanded = false
                        onHistory()
                    }
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
