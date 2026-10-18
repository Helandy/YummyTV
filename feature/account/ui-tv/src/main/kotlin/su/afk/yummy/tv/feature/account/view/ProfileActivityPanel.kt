package su.afk.yummy.tv.feature.account.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.domain.account.model.UserWatchHistoryDay
import su.afk.yummy.tv.feature.account.common.mapper.WATCH_HISTORY_GRID_COLUMNS
import su.afk.yummy.tv.feature.account.common.mapper.toWatchHistoryGrid
import su.afk.yummy.tv.feature.account.common.utils.weekdayLabels
import su.afk.yummy.tv.feature.account.presentation.R
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Секция «Активность», как на мобилке: стрик и календарь последних 4 недель.
 * Фокус на дне показывает его время и серии под календарём.
 */
@Composable
internal fun ProfileActivityPanel(
    history: List<UserWatchHistoryDay>,
    daysOnline: Int,
    modifier: Modifier = Modifier,
    /** Вход в календарь — сегодняшний день. */
    todayFocusRequester: FocusRequester? = null,
    /** Куда уходит фокус вверх с верхнего ряда календаря. */
    upFocusRequester: FocusRequester? = null,
    /** Куда уходит фокус вправо с последнего дня в ряду. */
    rightFocusRequester: FocusRequester? = null,
) {
    val firstDayOfWeek = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    val weekdays = remember(firstDayOfWeek) { firstDayOfWeek.weekdayLabels() }
    val cells = remember(history, firstDayOfWeek) {
        val zone = ZoneId.systemDefault()
        history.toWatchHistoryGrid(today = LocalDate.now(zone), zone = zone, firstDayOfWeek = firstDayOfWeek)
    }
    val todayIndex = cells.indexOfFirst { it.isToday }.coerceAtLeast(0)
    var selectedIndex by remember(cells) { mutableIntStateOf(todayIndex) }
    val selected = cells.getOrNull(selectedIndex) ?: cells[todayIndex]
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val scope = rememberCoroutineScope()
    // Навигация по календарю явная: дефолтный поиск фокуса внутри экрана аккаунта не срабатывает.
    val cellFocusRequesters = remember(cells.size) { List(cells.size) { FocusRequester() } }
    val focusRequesters = cellFocusRequesters.toMutableList().apply {
        if (todayFocusRequester != null && todayIndex in indices) this[todayIndex] = todayFocusRequester
    }

    fun requestCellFocus(index: Int): Boolean {
        if (cells.getOrNull(index)?.isFuture != false) return false
        return runCatching { focusRequesters[index].requestFocus() }.isSuccess
    }

    fun requestFocus(requester: FocusRequester?): Boolean =
        requester != null && runCatching { requester.requestFocus() }.isSuccess

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f))
            .focusGroup()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileSectionHeader(
                icon = Icons.Filled.Insights,
                title = stringResource(R.string.account_mobile_activity_title),
                modifier = Modifier.weight(1f),
            )
            if (daysOnline > 0) {
                StreakPill(daysOnline = daysOnline)
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
                        ProfileActivityCell(
                            cell = cell,
                            isSelected = index == selectedIndex,
                            onFocused = { selectedIndex = index },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequesters[index])
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    when (event.key) {
                                        Key.DirectionLeft -> if (columnIndex > 0) {
                                            requestCellFocus(index - 1)
                                        } else {
                                            // С первой колонки влево — в главное меню, как с остального экрана.
                                            val requester = mainMenuFocusRequester ?: return@onPreviewKeyEvent false
                                            scope.launch { requestFocusUntilTimeout(requester) }
                                            true
                                        }

                                        Key.DirectionRight -> {
                                            val moved = columnIndex < WATCH_HISTORY_GRID_COLUMNS - 1 &&
                                                requestCellFocus(index + 1)
                                            moved || requestFocus(rightFocusRequester)
                                            true
                                        }

                                        Key.DirectionUp -> {
                                            if (rowIndex > 0) {
                                                requestCellFocus(index - WATCH_HISTORY_GRID_COLUMNS)
                                            } else {
                                                requestFocus(upFocusRequester)
                                            }
                                            true
                                        }

                                        Key.DirectionDown -> {
                                            requestCellFocus(index + WATCH_HISTORY_GRID_COLUMNS)
                                            true
                                        }

                                        else -> false
                                    }
                                },
                        )
                    }
                }
            }
        }
        AnimatedContent(
            targetState = selected,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "activityDayDetails",
            modifier = Modifier.fillMaxWidth(),
        ) { cell ->
            ProfileActivityDayDetails(cell = cell)
        }
    }
}

@Composable
private fun StreakPill(daysOnline: Int) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
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
