@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.feature.account.R
import su.afk.yummy.tv.feature.account.utils.formatDate
import su.afk.yummy.tv.feature.account.utils.notificationTypeIcon

@Composable
internal fun NotificationRow(
    notification: ProfileNotification,
    onClick: () -> Unit,
    onRead: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    readModifier: Modifier = Modifier,
    deleteModifier: Modifier = Modifier,
    onReadDirectionRight: (() -> Boolean)? = null,
    onDeleteDirectionLeft: (() -> Boolean)? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isOpenable = notification.isNewEpisode && notification.animeSlug != null
    val rowModifier = modifier
        .fillMaxWidth()
        .tvFocusableClick(
            onClick = if (isOpenable) onClick else ({ }),
            interactionSource = interactionSource,
            shape = shape,
            focusedScale = 1.01f,
        )

    Surface(
        modifier = rowModifier,
        shape = shape,
        color = if (notification.viewed) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = notificationTypeIcon(notification.type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = notification.title.ifBlank { notification.type },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (notification.viewed) FontWeight.Medium else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = notification.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = notification.dateSeconds.formatDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            // IntrinsicSize.Min + aspectRatio: корзина ровно квадратная и той же высоты,
            // что и «Прочитать», без подгонки размеров руками.
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (!notification.viewed) {
                    AccountAction(
                        label = stringResource(R.string.account_mark_read),
                        onClick = onRead,
                        modifier = readModifier.width(170.dp),
                        onDirectionRight = onReadDirectionRight,
                    )
                }
                AccountAction(
                    label = stringResource(R.string.account_delete),
                    icon = Icons.Outlined.DeleteOutline,
                    onClick = onDelete,
                    modifier = deleteModifier
                        .fillMaxHeight()
                        .aspectRatio(1f),
                    iconOnly = true,
                    onDirectionLeft = onDeleteDirectionLeft,
                )
            }
        }
    }
}
