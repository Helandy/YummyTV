@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.feature.account.R
import su.afk.yummy.tv.feature.account.utils.formatDate

@Composable
internal fun NotificationRow(
    notification: ProfileNotification,
    onClick: () -> Unit,
    onRead: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isOpenable = notification.isNewEpisode && notification.animeSlug != null
    val rowModifier = if (isOpenable) {
        Modifier
            .fillMaxWidth()
            .tvFocusableClick(
                onClick = onClick,
                interactionSource = interactionSource,
                shape = shape,
                focusedScale = 1.01f,
            )
    } else {
        Modifier.fillMaxWidth()
    }

    SurfacePanel(modifier = rowModifier) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .padding(top = 7.dp)
                    .size(10.dp)
                    .background(
                        color = if (notification.viewed) Color.Transparent else MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = notification.title.ifBlank { notification.type },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (notification.viewed) FontWeight.SemiBold else FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!notification.viewed) {
                AccountAction(
                    label = stringResource(R.string.account_mark_read),
                    onClick = onRead,
                    modifier = Modifier.width(170.dp),
                )
            }
            AccountAction(
                label = stringResource(R.string.account_delete),
                onClick = onDelete,
                modifier = Modifier.width(140.dp),
            )
        }
    }
}
