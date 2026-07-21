package su.afk.yummy.tv.feature.comments.tv.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.comments.model.CommentSort
import su.afk.yummy.tv.feature.comments.tv.R
import su.afk.yummy.tv.feature.comments.tv.utils.labelRes

@Composable
internal fun CommentsHeader(
    selectedSort: CommentSort,
    initialFocusRequester: FocusRequester,
    onSortSelected: (CommentSort) -> Unit,
    onRefresh: () -> Unit,
) {
    val colors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(CommentSort.NEW, CommentSort.BEST, CommentSort.OLD).forEachIndexed { index, sort ->
            FilterChip(
                selected = selectedSort == sort,
                onClick = { onSortSelected(sort) },
                label = { Text(stringResource(sort.labelRes())) },
                colors = colors,
                modifier = if (index == 0) {
                    Modifier.focusRequester(initialFocusRequester)
                } else {
                    Modifier
                },
            )
        }
        FilterChip(
            selected = false,
            onClick = onRefresh,
            label = { Text(stringResource(R.string.comments_refresh)) },
            leadingIcon = {
                Icon(Icons.Filled.Refresh, contentDescription = null)
            },
            colors = colors,
        )
    }
}
