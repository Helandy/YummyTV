package su.afk.yummy.tv.feature.details.mobile.episodes.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeRange
import su.afk.yummy.tv.feature.details.mobile.episodes.utils.mobileChipLabel

/** Горизонтальная полоса чипов диапазонов (`1–50`, `51–100`, …) для длинных тайтлов. */
@Composable
internal fun EpisodeRangeMobileChips(
    ranges: List<EpisodeRange>,
    selectedRangeIndex: Int,
    onRangeSelected: (EpisodeRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedRangeIndex, ranges.size) {
        if (selectedRangeIndex in ranges.indices) {
            listState.animateScrollToItem(selectedRangeIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(ranges, key = { _, range -> range.startIndex }) { index, range ->
            FilterChip(
                selected = index == selectedRangeIndex,
                onClick = { onRangeSelected(range) },
                label = { Text(text = range.mobileChipLabel(), maxLines = 1) },
            )
        }
    }
}
