package su.afk.yummy.tv.feature.collection.mobile.view

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.collection.model.CollectionSort
import su.afk.yummy.tv.feature.collection.utils.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollectionsSortSelector(
    selectedSort: CollectionSort,
    onSortSelected: (CollectionSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sorts = CollectionSort.entries
    val colors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary,
        activeBorderColor = MaterialTheme.colorScheme.primary,
        inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
    )
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        sorts.forEachIndexed { index, sort ->
            SegmentedButton(
                selected = selectedSort == sort,
                onClick = { onSortSelected(sort) },
                shape = SegmentedButtonDefaults.itemShape(index, sorts.size),
                colors = colors,
                label = { Text(stringResource(sort.labelRes())) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionsSortSelectorPreview() = ScreenPreviewTheme {
    CollectionsSortSelector(selectedSort = CollectionSort.BEST, onSortSelected = {})
}
