package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.account.utils.formatDate
import su.afk.yummy.tv.feature.account.mobile.account.utils.notificationTypeIcon

@Composable
internal fun AccountMobileNotificationRow(
    notification: ProfileNotification,
    onClick: () -> Unit,
    onRead: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOpenable = notification.isNewEpisode && notification.animeSlug != null
    val openLabel = stringResource(R.string.account_open)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isOpenable) {
                    Modifier
                        .clickable(onClick = onClick)
                        .semantics { contentDescription = openLabel }
                } else {
                    Modifier
                }
            ),
        shape = MaterialTheme.shapes.large,
        color = if (notification.viewed) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
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
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = notification.title.ifBlank { notification.type },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.viewed) FontWeight.Medium else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = notification.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = notification.dateSeconds.formatDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!notification.viewed) {
                    IconButton(
                        onClick = onRead,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = stringResource(R.string.account_mark_read),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.account_delete),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AccountMobileNotificationRowPreview() {
    ScreenPreviewTheme {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AccountMobileNotificationRow(
                notification = previewNotification(
                    id = 1,
                    viewed = false,
                    title = "Новая серия в аниме «Военная хроника маленькой девочки 2»!",
                    text = "В аниме Военная хроника маленькой девочки 2 вышла серия № 10 Alloha, Озвучка AniLibria",
                ),
                onClick = {},
                onRead = {},
                onDelete = {},
            )
            AccountMobileNotificationRow(
                notification = previewNotification(
                    id = 2,
                    viewed = true,
                    title = "Новая серия в аниме «Табакошка»!",
                    text = "В аниме Табакошка вышла серия № 10 Kodik, Озвучка РуАниме / DEEP",
                ),
                onClick = {},
                onRead = {},
                onDelete = {},
            )
            AccountMobileNotificationRow(
                notification = previewNotification(
                    id = 3,
                    viewed = false,
                    title = "Новый комментарий",
                    text = "Вам ответили в обсуждении",
                    type = "comment",
                    isNewEpisode = false,
                    animeSlug = null,
                ),
                onClick = {},
                onRead = {},
                onDelete = {},
            )
        }
    }
}

private fun previewNotification(
    id: Int,
    viewed: Boolean,
    title: String,
    text: String,
    type: String = "anime_episode",
    isNewEpisode: Boolean = true,
    animeSlug: String? = "youjo-senki",
) = ProfileNotification(
    id = id,
    dateSeconds = 1_757_500_000L,
    title = title,
    text = text,
    clickUri = "",
    type = type,
    subType = "",
    viewed = viewed,
    objectId = null,
    animeSlug = animeSlug,
    isNewEpisode = isNewEpisode,
)
