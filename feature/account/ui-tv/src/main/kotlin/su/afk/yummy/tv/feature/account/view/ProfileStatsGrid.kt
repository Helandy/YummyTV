@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.feature.account.R
import su.afk.yummy.tv.feature.account.model.ProfileStatSlice
import su.afk.yummy.tv.feature.account.utils.averageRatingLabel
import su.afk.yummy.tv.feature.account.utils.genreCountSlices
import su.afk.yummy.tv.feature.account.utils.listDurationSlices
import su.afk.yummy.tv.feature.account.utils.positiveValueSum
import su.afk.yummy.tv.feature.account.utils.ratingCountSlices
import su.afk.yummy.tv.feature.account.utils.toProfileHoursLabel
import su.afk.yummy.tv.feature.account.utils.watchStatSlices

@Composable
internal fun ProfileStatsGrid(
    summary: UserProfileSummary,
    stats: UserStats?,
    focusRequester: FocusRequester? = null,
    bottomStartFocusRequester: FocusRequester? = null,
    topExitFocusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {},
    onExitRight: () -> Boolean = { false },
    onExitDown: () -> Boolean = { false },
    modifier: Modifier = Modifier,
) {
    val pages = buildList {
        add(
            ProfileStatsPageModel(
                title = stringResource(R.string.account_profile_watch_time_title),
                slices = summary.watchStatSlices(),
                valueType = ProfileStatsValueType.DURATION,
            )
        )
        stats?.let {
            add(
                ProfileStatsPageModel(
                    title = stringResource(R.string.account_profile_list_duration_title),
                    slices = it.listDurationSlices(),
                    valueType = ProfileStatsValueType.DURATION,
                )
            )
            add(
                ProfileStatsPageModel(
                    title = stringResource(R.string.account_profile_genres_title),
                    slices = it.genreCountSlices(),
                    valueType = ProfileStatsValueType.COUNT,
                )
            )
            add(
                ProfileStatsPageModel(
                    title = stringResource(
                        R.string.account_profile_ratings_title,
                        it.averageRatingLabel(),
                    ),
                    slices = it.ratingCountSlices(),
                    valueType = ProfileStatsValueType.COUNT,
                    compactLegend = true,
                )
            )
        }
    }

    val internalFocusRequesters = remember(pages.size) {
        List(pages.size) { FocusRequester() }
    }
    val cardFocusRequesters = internalFocusRequesters.toMutableList().apply {
        if (isNotEmpty() && focusRequester != null) {
            this[0] = focusRequester
        }
        if (size > PROFILE_STATS_BOTTOM_START_INDEX && bottomStartFocusRequester != null) {
            this[PROFILE_STATS_BOTTOM_START_INDEX] = bottomStartFocusRequester
        }
    }
    var focusedCardIndex by remember(pages.size) { mutableStateOf<Int?>(null) }
    var gridFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                val nextFocused = it.isFocused || it.hasFocus
                if (gridFocused != nextFocused) {
                    gridFocused = nextFocused
                    onFocusChanged(nextFocused)
                }
            }
            .focusGroup()
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        pages.chunked(PROFILE_STATS_COLUMNS).forEachIndexed { rowIndex, rowPages ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                rowPages.forEachIndexed { columnIndex, page ->
                    val pageIndex = rowIndex * PROFILE_STATS_COLUMNS + columnIndex
                    ProfileStatsCard(
                        page = page,
                        focused = focusedCardIndex == pageIndex,
                        modifier = Modifier
                            .weight(1f)
                            .profileStatsBlockFocus(
                                pageIndex = pageIndex,
                                focusRequester = cardFocusRequesters[pageIndex],
                                focusRequesters = cardFocusRequesters,
                                onFocused = {
                                    focusedCardIndex = pageIndex
                                },
                                onUnfocused = {
                                    if (focusedCardIndex == pageIndex) {
                                        focusedCardIndex = null
                                    }
                                },
                                topExitFocusRequester = topExitFocusRequester,
                                onExitRight = onExitRight,
                                onExitDown = onExitDown,
                            ),
                    )
                }
                if (rowPages.size < PROFILE_STATS_COLUMNS) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProfileStatsCard(
    page: ProfileStatsPageModel,
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    val positiveSlices = page.slices.filter { it.value > 0L }
    val totalValue = positiveSlices.positiveValueSum()
    val shape = RoundedCornerShape(12.dp)
    val containerColor = if (focused) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.055f)
    }

    Column(
        modifier = modifier
            .height(252.dp)
            .graphicsLayer {
                val scale = if (focused) 1.015f else 1f
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(containerColor)
            .border(
                width = if (focused) 3.dp else 2.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = page.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (totalValue <= 0L) {
            ProfileStatsEmptyState(modifier = Modifier.weight(1f))
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProfileStatsDonutChart(
                    slices = positiveSlices,
                    totalLabel = page.valueType.totalLabel(totalValue),
                    percentLabel = stringResource(R.string.account_profile_percent_full),
                )
                ProfileStatsLegend(
                    slices = page.slices,
                    valueType = page.valueType,
                    compact = page.compactLegend,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun Modifier.profileStatsBlockFocus(
    pageIndex: Int,
    focusRequester: FocusRequester,
    focusRequesters: List<FocusRequester>,
    onFocused: () -> Unit,
    onUnfocused: () -> Unit,
    topExitFocusRequester: FocusRequester?,
    onExitRight: () -> Boolean,
    onExitDown: () -> Boolean,
): Modifier {
    fun requestFocusAt(index: Int): Boolean {
        val requester = focusRequesters.getOrNull(index) ?: return false
        return runCatching { requester.requestFocus() }.isSuccess
    }

    val leftIndex = pageIndex - 1
    val rightIndex = pageIndex + 1
    val upIndex = pageIndex - PROFILE_STATS_COLUMNS
    val downIndex = pageIndex + PROFILE_STATS_COLUMNS

    return this
        .focusRequester(focusRequester)
        .focusProperties {
            if (leftIndex >= 0 && pageIndex % PROFILE_STATS_COLUMNS != 0) {
                left = focusRequesters[leftIndex]
            }
            if (rightIndex < focusRequesters.size && rightIndex % PROFILE_STATS_COLUMNS != 0) {
                right = focusRequesters[rightIndex]
            }
            if (upIndex >= 0) {
                up = focusRequesters[upIndex]
            } else {
                topExitFocusRequester?.let { up = it }
            }
            if (downIndex < focusRequesters.size) {
                down = focusRequesters[downIndex]
            }
        }
        .onFocusChanged {
            if (it.isFocused) {
                onFocused()
            } else if (!it.hasFocus) {
                onUnfocused()
            }
        }
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.DirectionLeft -> {
                    if (leftIndex >= 0 && pageIndex % PROFILE_STATS_COLUMNS != 0) {
                        requestFocusAt(leftIndex)
                    } else {
                        false
                    }
                }

                Key.DirectionRight -> {
                    if (rightIndex < focusRequesters.size && rightIndex % PROFILE_STATS_COLUMNS != 0) {
                        requestFocusAt(rightIndex)
                    } else {
                        onExitRight()
                    }
                }

                Key.DirectionUp -> {
                    if (upIndex >= 0) {
                        requestFocusAt(upIndex)
                    } else {
                        topExitFocusRequester?.let {
                            runCatching { it.requestFocus() }.isSuccess
                        } ?: false
                    }
                }

                Key.DirectionDown -> {
                    if (downIndex < focusRequesters.size) {
                        requestFocusAt(downIndex)
                    } else {
                        onExitDown()
                    }
                }

                else -> false
            }
        }
        .focusable()
}

@Composable
private fun ProfileStatsDonutChart(
    slices: List<ProfileStatSlice>,
    totalLabel: String,
    percentLabel: String,
    modifier: Modifier = Modifier,
) {
    val totalValue = slices.positiveValueSum()
    Box(
        modifier = modifier.size(132.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (totalValue <= 0L) {
                drawCircle(color = Color.White.copy(alpha = 0.12f))
                return@Canvas
            }
            var startAngle = -90f
            slices.forEach { slice ->
                val sweepAngle = 360f * slice.value.toFloat() / totalValue.toFloat()
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                )
                startAngle += sweepAngle
            }
        }
        Column(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = totalLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = percentLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ProfileStatsLegend(
    slices: List<ProfileStatSlice>,
    valueType: ProfileStatsValueType,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        slices.forEach { slice ->
            Row(
                modifier = Modifier.widthIn(
                    min = if (compact) 58.dp else 94.dp,
                    max = if (compact) 80.dp else 150.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(slice.color),
                )
                Text(
                    text = slice.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = valueType.valueLabel(slice.value),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ProfileStatsEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.account_profile_stats_empty),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private data class ProfileStatsPageModel(
    val title: String,
    val slices: List<ProfileStatSlice>,
    val valueType: ProfileStatsValueType,
    val compactLegend: Boolean = false,
)

private enum class ProfileStatsValueType {
    DURATION,
    COUNT,
}

@Composable
private fun ProfileStatsValueType.totalLabel(value: Long): String =
    when (this) {
        ProfileStatsValueType.DURATION -> value.toProfileHoursLabel()
        ProfileStatsValueType.COUNT -> value.toString()
    }

@Composable
private fun ProfileStatsValueType.valueLabel(value: Long): String =
    when (this) {
        ProfileStatsValueType.DURATION -> value.toProfileHoursLabel()
        ProfileStatsValueType.COUNT -> value.toString()
    }

private const val PROFILE_STATS_COLUMNS = 2
private const val PROFILE_STATS_BOTTOM_START_INDEX = PROFILE_STATS_COLUMNS
