@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.feature.account.R
import su.afk.yummy.tv.feature.account.account.model.ProfileStatsPageModel
import su.afk.yummy.tv.feature.account.account.model.ProfileStatsValueType
import su.afk.yummy.tv.feature.account.utils.averageRatingLabel
import su.afk.yummy.tv.feature.account.utils.genreCountSlices
import su.afk.yummy.tv.feature.account.utils.listDurationSlices
import su.afk.yummy.tv.feature.account.utils.ratingCountSlices
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

internal fun Modifier.profileStatsBlockFocus(
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

internal const val PROFILE_STATS_COLUMNS = 2
internal const val PROFILE_STATS_BOTTOM_START_INDEX = PROFILE_STATS_COLUMNS
