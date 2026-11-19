package su.afk.yummy.tv.feature.schedule.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.schedule.model.ScheduleDayUi

private const val INACTIVE_CONTAINER_ALPHA = 0.06f
private const val INACTIVE_BADGE_ALPHA = 0.22f
private const val ACTIVE_BADGE_ALPHA = 0.18f

@Composable
internal fun ScheduleMobileDateChip(
    group: ScheduleDayUi,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    selectionFraction: Float = if (selected) 1f else 0f,
) {
    val fraction = selectionFraction.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(10.dp)
    val inactiveOverlay = MaterialTheme.colorScheme.onSurface.copy(
        alpha = INACTIVE_CONTAINER_ALPHA * (1f - fraction),
    )
    val contentColor = lerp(
        MaterialTheme.colorScheme.onSurfaceVariant,
        MaterialTheme.colorScheme.onPrimary,
        fraction,
    )
    val badgeColor = lerp(
        MaterialTheme.colorScheme.primary.copy(alpha = INACTIVE_BADGE_ALPHA),
        MaterialTheme.colorScheme.onPrimary.copy(alpha = ACTIVE_BADGE_ALPHA),
        fraction,
    )

    Box(
        modifier = modifier
            .width(78.dp)
            .height(72.dp)
            .clip(shape)
            .background(inactiveOverlay, shape)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onSelected,
            )
            .padding(8.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    text = group.weekdayLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .height(22.dp)
                        .widthIn(min = 22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(color = badgeColor)
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = group.items.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = group.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = contentColor,
                maxLines = 1,
            )
        }
        if (group.isToday) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(3.dp)
                    .size(5.dp)
                    .background(contentColor, CircleShape),
            )
        }
    }
}
