package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.domain.account.model.UserWatchHistoryDay
import su.afk.yummy.tv.feature.account.common.mapper.WATCH_HISTORY_GRID_COLUMNS
import su.afk.yummy.tv.feature.account.common.mapper.toWatchHistoryGrid
import su.afk.yummy.tv.feature.account.common.model.WatchHistoryCell
import su.afk.yummy.tv.feature.account.common.utils.detailsLabel
import su.afk.yummy.tv.feature.account.common.utils.weekdayLabels
import su.afk.yummy.tv.feature.account.presentation.R
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/** Секция «Активность»: стрик и календарь последних 4 недель с выбором дня. */
@Composable
internal fun AccountMobileProfileHeatmap(
    history: List<UserWatchHistoryDay>,
    daysOnline: Int,
    modifier: Modifier = Modifier,
) {
    val firstDayOfWeek = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    val weekdays = remember(firstDayOfWeek) { firstDayOfWeek.weekdayLabels() }
    val cells = remember(history, firstDayOfWeek) {
        val zone = ZoneId.systemDefault()
        history.toWatchHistoryGrid(today = LocalDate.now(zone), zone = zone, firstDayOfWeek = firstDayOfWeek)
    }
    val todayIndex = cells.indexOfFirst { it.isToday }.coerceAtLeast(0)
    var selectedIndex by rememberSaveable(history) { mutableIntStateOf(todayIndex) }
    val selected = cells.getOrNull(selectedIndex) ?: cells[todayIndex]

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountMobileSectionLabel(
                icon = Icons.Filled.Insights,
                title = stringResource(R.string.account_mobile_activity_title),
                modifier = Modifier.weight(1f),
            )
            if (daysOnline > 0) {
                AccountMobileStreakPill(daysOnline = daysOnline)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                weekdays.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
            cells.chunked(WATCH_HISTORY_GRID_COLUMNS).forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEachIndexed { columnIndex, cell ->
                        val index = rowIndex * WATCH_HISTORY_GRID_COLUMNS + columnIndex
                        AccountMobileHeatmapCell(
                            cell = cell,
                            isSelected = index == selectedIndex,
                            onClick = { selectedIndex = index },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        AnimatedContent(
            targetState = selected,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "heatmapDayDetails",
            modifier = Modifier.fillMaxWidth(),
        ) { cell ->
            AccountMobileHeatmapDayDetails(cell = cell)
        }
    }
}

@Composable
private fun AccountMobileStreakPill(daysOnline: Int) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Whatshot,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = pluralStringResource(R.plurals.account_mobile_streak_days, daysOnline, daysOnline),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun AccountMobileHeatmapCell(
    cell: WatchHistoryCell,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val background = when {
        cell.isFuture -> Color.Transparent
        cell.intensity > 0f -> YummySemanticColors.Dislike.copy(alpha = cell.intensity)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    }
    val textColor = when {
        cell.isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        cell.intensity > 0.5f -> Color.White
        cell.intensity > 0f -> Color.White.copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    val description = if (cell.isFuture) "" else cell.detailsLabel()
    Box(
        modifier = modifier
            .aspectRatio(1.15f)
            .clip(shape)
            .background(background)
            .then(
                when {
                    isSelected -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                    cell.isFuture -> Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        shape,
                    )

                    else -> Modifier
                },
            )
            .then(
                if (cell.isFuture) {
                    Modifier
                } else {
                    Modifier
                        .clickable(role = Role.Button, onClick = onClick)
                        .semantics {
                            contentDescription = description
                            selected = isSelected
                        }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = cell.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (cell.isToday && cell.intensity == 0f) MaterialTheme.colorScheme.primary else textColor,
        )
    }
}
