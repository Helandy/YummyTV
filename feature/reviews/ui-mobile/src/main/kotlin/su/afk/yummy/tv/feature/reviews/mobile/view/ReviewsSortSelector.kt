package su.afk.yummy.tv.feature.reviews.mobile.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.feature.reviews.mobile.utils.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReviewsSortSelector(
    sorts: List<ReviewSort>,
    selectedSort: ReviewSort,
    onSortSelected: (ReviewSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sortTabColors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary,
        activeBorderColor = MaterialTheme.colorScheme.primary,
        inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
    )
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        sorts.forEachIndexed { index, sort ->
            SegmentedButton(
                selected = selectedSort == sort,
                onClick = { onSortSelected(sort) },
                shape = SegmentedButtonDefaults.itemShape(index, sorts.size),
                colors = sortTabColors,
                label = { Text(sort.label()) },
            )
        }
    }
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
