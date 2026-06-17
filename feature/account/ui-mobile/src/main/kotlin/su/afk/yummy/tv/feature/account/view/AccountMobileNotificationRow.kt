@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.feature.account.account.mobile.utils.formatDate
import su.afk.yummy.tv.feature.account.mobile.R

@Composable
internal fun AccountMobileNotificationRow(
    notification: ProfileNotification,
    onClick: () -> Unit,
    onRead: () -> Unit,
    onDelete: () -> Unit,
) {
    val isOpenable = notification.isNewEpisode && notification.animeSlug != null
    AccountMobileSurfacePanel(
        modifier = if (isOpenable) Modifier.clickable(onClick = onClick) else Modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(
                            color = if (notification.viewed) Color.Transparent else MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = notification.title.ifBlank { notification.type },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.viewed) FontWeight.SemiBold else FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = notification.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = notification.dateSeconds.formatDate(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isOpenable) {
                    TextButton(onClick = onClick) {
                        Text(stringResource(R.string.account_open))
                    }
                }
                if (!notification.viewed) {
                    OutlinedButton(onClick = onRead) {
                        Text(stringResource(R.string.account_mark_read))
                    }
                }
                OutlinedButton(onClick = onDelete) {
                    Text(stringResource(R.string.account_delete))
                }
            }
        }
    }
}
