@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.ProfileNotification

@Composable
internal fun NotificationMiniRow(notification: ProfileNotification) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = notification.title.ifBlank { notification.type },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (notification.viewed) FontWeight.SemiBold else FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = notification.text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
