package su.afk.yummy.tv.feature.collection.mobile.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTextTabs
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.collection.model.CollectionSort
import su.afk.yummy.tv.feature.collection.utils.labelRes

@Composable
internal fun CollectionsSortSelector(
    selectedSort: CollectionSort,
    onSortSelected: (CollectionSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sorts = CollectionSort.entries
    MobileTextTabs(
        titles = sorts.map { stringResource(it.labelRes()) },
        selectedIndex = sorts.indexOf(selectedSort).coerceAtLeast(0),
        onSelected = { onSortSelected(sorts[it]) },
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true)
@Composable
private fun CollectionsSortSelectorPreview() = ScreenPreviewTheme {
    CollectionsSortSelector(selectedSort = CollectionSort.BEST, onSortSelected = {})
}
