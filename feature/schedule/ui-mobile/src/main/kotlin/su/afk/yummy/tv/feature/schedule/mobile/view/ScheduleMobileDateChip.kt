package su.afk.yummy.tv.feature.schedule.mobile.view

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTabColorText
import su.afk.yummy.tv.feature.schedule.model.ScheduleDayUi

private const val INACTIVE_CONTAINER_ALPHA = 0.06f
private const val INACTIVE_BADGE_ALPHA = 0.22f
private const val ACTIVE_BADGE_ALPHA = 0.18f
private val CHIP_CORNER_RADIUS = 10.dp
private val BADGE_CORNER_RADIUS = 11.dp

@Composable
internal fun ScheduleMobileDateChip(
    group: ScheduleDayUi,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    selectionFraction: () -> Float = { if (selected) 1f else 0f },
) {
    val colors = MaterialTheme.colorScheme
    val onSurface = colors.onSurface
    val unselectedContent = colors.onSurfaceVariant
    val selectedContent = colors.onPrimary
    val inactiveBadge = colors.primary.copy(alpha = INACTIVE_BADGE_ALPHA)
    val activeBadge = colors.onPrimary.copy(alpha = ACTIVE_BADGE_ALPHA)
    val contentColor = remember(selectionFraction, unselectedContent, selectedContent) {
        ColorProducer {
            lerp(unselectedContent, selectedContent, selectionFraction().coerceIn(0f, 1f))
        }
    }

    Box(
        modifier = modifier
            .width(78.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(CHIP_CORNER_RADIUS))
            .drawBehind {
                val fraction = selectionFraction().coerceIn(0f, 1f)
                val alpha = INACTIVE_CONTAINER_ALPHA * (1f - fraction)
                if (alpha > 0f) {
                    drawRoundRect(
                        color = onSurface.copy(alpha = alpha),
                        cornerRadius = CornerRadius(CHIP_CORNER_RADIUS.toPx()),
                    )
                }
            }
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
                MobileTabColorText(
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    text = group.weekdayLabel,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .height(22.dp)
                        .widthIn(min = 22.dp)
                        .drawBehind {
                            val fraction = selectionFraction().coerceIn(0f, 1f)
                            drawRoundRect(
                                color = lerp(inactiveBadge, activeBadge, fraction),
                                cornerRadius = CornerRadius(BADGE_CORNER_RADIUS.toPx()),
                            )
                        }
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MobileTabColorText(
                        text = group.items.size.toString(),
                        color = contentColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
            MobileTabColorText(
                text = group.date.dayOfMonth.toString(),
                color = contentColor,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
            )
        }
        if (group.isToday) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(3.dp)
                    .size(5.dp)
                    .drawBehind {
                        drawCircle(color = contentColor())
                    },
            )
        }
    }
}
