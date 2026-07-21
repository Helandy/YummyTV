package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.FriendshipStatus

@Composable
internal fun UserProfileActions(
    showMessage: Boolean,
    showFriendship: Boolean,
    showComments: Boolean,
    isAuthorized: Boolean,
    friendshipStatus: FriendshipStatus,
    friendshipLoading: Boolean,
    messageLabel: String,
    friendshipLabel: String,
    commentsLabel: String,
    onMessageClick: () -> Unit,
    onFriendshipClick: () -> Unit,
    onCommentsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showMessage) {
            ProfileAction(
                icon = Icons.Filled.MailOutline,
                label = messageLabel,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onMessageClick,
                modifier = Modifier.weight(1f),
            )
        }
        if (showFriendship) {
            ProfileAction(
                icon = friendshipStatus.actionIcon(isAuthorized),
                label = friendshipLabel,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                enabled = !friendshipLoading,
                loading = friendshipLoading,
                onClick = onFriendshipClick,
                modifier = Modifier.weight(1f),
            )
        }
        if (showComments) {
            ProfileAction(
                icon = Icons.Filled.ChatBubbleOutline,
                label = commentsLabel,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onCommentsClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProfileAction(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 76.dp),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(21.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private fun FriendshipStatus.actionIcon(isAuthorized: Boolean): ImageVector = when {
    !isAuthorized -> Icons.AutoMirrored.Filled.Login
    this == FriendshipStatus.FRIENDS ||
            this == FriendshipStatus.FOLLOWING ||
            this == FriendshipStatus.SENT_REQUESTS -> Icons.Filled.PersonRemove

    else -> Icons.Filled.PersonAdd
}
