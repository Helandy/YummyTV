package su.afk.yummy.tv.feature.reviews.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTextTabs
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.feature.reviews.utils.labelRes

@Composable
internal fun ReviewsSortSelector(
    sorts: List<ReviewSort>,
    selectedSort: ReviewSort,
    onSortSelected: (ReviewSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    MobileTextTabs(
        titles = sorts.map { stringResource(it.labelRes()) },
        selectedIndex = sorts.indexOf(selectedSort).coerceAtLeast(0),
        onSelected = { onSortSelected(sorts[it]) },
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    )
}

@Preview(name = "General feed", showBackground = true)
@Composable
private fun ReviewsSortSelectorGeneralPreview() = ScreenPreviewTheme {
    ReviewsSortSelector(
        sorts = listOf(ReviewSort.NEW, ReviewSort.TOP),
        selectedSort = ReviewSort.NEW,
        onSortSelected = {},
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(name = "Anime reviews", showBackground = true)
@Composable
private fun ReviewsSortSelectorAnimePreview() = ScreenPreviewTheme {
    ReviewsSortSelector(
        sorts = ReviewSort.entries,
        selectedSort = ReviewSort.TOP,
        onSortSelected = {},
        modifier = Modifier.fillMaxWidth(),
    )
}
