package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.TvCenteredCarouselBringIntoViewSpec
import su.afk.yummy.tv.core.designsystem.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.tv.TvChip
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeRange
import su.afk.yummy.tv.feature.details.episodes.utils.tvChipLabel

/** Горизонтальная полоса чипов диапазонов (`1–50`, `51–100`, …) для длинных тайтлов на ТВ. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EpisodeRangeChips(
    ranges: List<EpisodeRange>,
    selectedRangeIndex: Int,
    onRangeSelected: (EpisodeRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val chipFocusRequesters = remember(ranges) { List(ranges.size) { FocusRequester() } }
    val activeChipFocusRequester = chipFocusRequesters.getOrNull(selectedRangeIndex) ?: FocusRequester.Default
    var rowHasFocus by remember { mutableStateOf(false) }

    LaunchedEffect(selectedRangeIndex, ranges.size) {
        if (!rowHasFocus && selectedRangeIndex in ranges.indices) {
            listState.scrollToItem(selectedRangeIndex)
        }
    }

    CompositionLocalProvider(LocalBringIntoViewSpec provides TvCenteredCarouselBringIntoViewSpec) {
        LazyRow(
            state = listState,
            modifier = modifier
                .onFocusChanged { rowHasFocus = it.hasFocus }
                .tvFocusRestorer(
                    fallback = activeChipFocusRequester,
                    enabled = ranges.isNotEmpty(),
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        ) {
            itemsIndexed(ranges, key = { _, range -> range.startIndex }) { index, range ->
                TvChip(
                    label = range.tvChipLabel(),
                    selected = index == selectedRangeIndex,
                    onClick = { onRangeSelected(range) },
                    modifier = Modifier.focusRequester(chipFocusRequesters[index]),
                )
            }
        }
    }
}
