package su.afk.yummy.tv.feature.account.mobile.account.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.domain.account.model.FriendshipStatus

internal fun FriendshipStatus.actionIcon(isAuthorized: Boolean): ImageVector = when {
    !isAuthorized -> Icons.AutoMirrored.Filled.Login
    this == FriendshipStatus.FRIENDS ||
            this == FriendshipStatus.FOLLOWING ||
            this == FriendshipStatus.SENT_REQUESTS -> Icons.Filled.PersonRemove

    else -> Icons.Filled.PersonAdd
}
