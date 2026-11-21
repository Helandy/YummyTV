package su.afk.yummy.tv.feature.details.mobile.episodes.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTextTabs
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeRange
import su.afk.yummy.tv.feature.details.episodes.utils.chipLabel

/** Плоские текстовые вкладки диапазонов (`1–50`, `51–100`, …) для длинных тайтлов. */
@Composable
internal fun EpisodeRangeMobileTabs(
    ranges: List<EpisodeRange>,
    selectedRangeIndex: Int,
    onRangeSelected: (index: Int, range: EpisodeRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titles = ranges.map { it.chipLabel() }
    MobileTextTabs(
        titles = titles,
        selectedIndex = selectedRangeIndex,
        onSelected = { index -> ranges.getOrNull(index)?.let { onRangeSelected(index, it) } },
        modifier = modifier,
        scrollable = true,
    )
}
