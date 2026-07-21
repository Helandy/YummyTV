package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import su.afk.yummy.tv.domain.account.model.UserListWatchStat
import su.afk.yummy.tv.feature.account.mobile.account.utils.toDurationLabel

@Composable
internal fun AccountMobileListStatCard(
    stat: UserListWatchStat,
    modifier: Modifier = Modifier,
) {
    AccountMobileSurfacePanel(modifier = modifier) {
        Text(
            text = stat.seconds.toDurationLabel(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stat.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
