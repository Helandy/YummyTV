package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.search.R

@Composable
internal fun FilterButton(
    activeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (activeCount > 0) {
        stringResource(R.string.search_filters_with_count, activeCount)
    } else {
        stringResource(R.string.search_filters)
    }
    SelectableRow(
        label = label,
        selected = activeCount > 0,
        onClick = onClick,
        modifier = modifier.widthIn(min = 148.dp),
    )
}
