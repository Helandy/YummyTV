package su.afk.yummy.tv.feature.main.mobile.view

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.feature.main.mobile.model.MobileMenuItem

@Composable
internal fun <T> MobileMainNavigationIcon(item: MobileMenuItem<T>, selected: Boolean) {
    BadgedBox(
        badge = {
            if (item.badgeCount > 0) {
                Badge { Text(item.badgeCount.toString()) }
            }
        },
    ) {
        if (item.avatarUrl.isNotBlank()) {
            AsyncImage(
                model = item.avatarUrl,
                contentDescription = item.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(AvatarIconSize)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = CircleShape,
                    ),
            )
        } else {
            Icon(item.icon, contentDescription = item.label)
        }
    }
}

private val AvatarIconSize = 26.dp
