package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.R
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.model.anime.CardListStatus

/** Высота плашки статуса; оверлеи постера (год и т.п.) поднимаются на неё. */
val ListStatusBadgeHeight = 22.dp

/** Плашка «в каком списке пользователя лежит тайтл» на всю нижнюю кромку постера. */
@Composable
fun ListStatusBadge(
    status: CardListStatus,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ListStatusBadgeHeight)
            .background(status.badgeColor().copy(alpha = 0.70f)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp),
        ) {
            Icon(
                imageVector = status.icon(),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(status.labelRes()),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun CardListStatus.labelRes(): Int = when (this) {
    CardListStatus.WATCHING -> R.string.core_list_status_watching
    CardListStatus.PLANNED -> R.string.core_list_status_planned
    CardListStatus.COMPLETED -> R.string.core_list_status_completed
    CardListStatus.POSTPONED -> R.string.core_list_status_postponed
    CardListStatus.DROPPED -> R.string.core_list_status_dropped
}

internal fun CardListStatus.badgeColor(): Color = when (this) {
    CardListStatus.WATCHING -> YummySemanticColors.StatusWatching
    CardListStatus.PLANNED -> YummySemanticColors.StatusPlanned
    CardListStatus.COMPLETED -> YummySemanticColors.StatusCompleted
    CardListStatus.POSTPONED -> YummySemanticColors.StatusPostponed
    CardListStatus.DROPPED -> YummySemanticColors.StatusDropped
}
