package su.afk.yummy.tv.feature.bloggers.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoSort
import su.afk.yummy.tv.feature.bloggers.list.BloggerVideosListState
import su.afk.yummy.tv.feature.bloggers.mobile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BloggerVideosFilters(
    state: BloggerVideosListState.State,
    onCategorySelected: (String) -> Unit,
    onBloggerSelected: (Int?) -> Unit,
    onSortSelected: (BloggerVideoSort) -> Unit,
    onOpenBlogger: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFilters by remember { mutableStateOf(false) }
    val selectedCategory = state.categories.firstOrNull { it.id == state.selectedCategory }
    val selectedBlogger = state.bloggers.firstOrNull { it.id == state.selectedBloggerId }
    val activeFilters = listOfNotNull(selectedCategory?.title, selectedBlogger?.nickname)
    val sortTabColors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primary,
        activeContentColor = MaterialTheme.colorScheme.onPrimary,
        activeBorderColor = MaterialTheme.colorScheme.primary,
        inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
    )

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            BloggerVideoSort.entries.forEachIndexed { index, sort ->
                SegmentedButton(
                    selected = state.sort == sort,
                    onClick = { onSortSelected(sort) },
                    shape = SegmentedButtonDefaults.itemShape(index, BloggerVideoSort.entries.size),
                    colors = sortTabColors,
                    label = { Text(stringResource(sort.labelRes())) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = activeFilters.joinToString(" · "),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FilledTonalButton(onClick = { showFilters = true }) {
                Icon(Icons.Default.FilterList, contentDescription = null)
                Text(
                    text = if (activeFilters.isEmpty()) {
                        stringResource(R.string.blogger_filters)
                    } else {
                        stringResource(R.string.blogger_filters_count, activeFilters.size)
                    },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }

    if (showFilters) {
        BloggerVideosFiltersSheet(
            state = state,
            onDismiss = { showFilters = false },
            onCategorySelected = onCategorySelected,
            onBloggerSelected = onBloggerSelected,
            onOpenBlogger = onOpenBlogger,
            onReset = onReset,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BloggerVideosFiltersSheet(
    state: BloggerVideosListState.State,
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onBloggerSelected: (Int?) -> Unit,
    onOpenBlogger: (Int) -> Unit,
    onReset: () -> Unit,
) {
    val hasActiveFilters = state.selectedCategory != "all" || state.selectedBloggerId != null
    val filterChipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.blogger_filters_title),
                    fontWeight = FontWeight.Bold,
                )
                TextButton(
                    onClick = onReset,
                    enabled = hasActiveFilters,
                ) {
                    Text(stringResource(R.string.blogger_filters_reset))
                }
            }

            FilterSectionTitle(stringResource(R.string.blogger_filters_category))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FilterChip(
                    selected = state.selectedCategory == "all",
                    onClick = { onCategorySelected("all") },
                    label = { Text(stringResource(R.string.blogger_videos_all)) },
                    shape = RoundedCornerShape(6.dp),
                    colors = filterChipColors,
                )
                state.categories.forEach { category ->
                    FilterChip(
                        selected = state.selectedCategory == category.id,
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.title) },
                        shape = RoundedCornerShape(6.dp),
                        colors = filterChipColors,
                    )
                }
            }

            HorizontalDivider()
            FilterSectionTitle(stringResource(R.string.blogger_filters_blogger))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FilterChip(
                    selected = state.selectedBloggerId == null,
                    onClick = { onBloggerSelected(null) },
                    label = { Text(stringResource(R.string.blogger_videos_all_bloggers)) },
                    shape = RoundedCornerShape(6.dp),
                    colors = filterChipColors,
                )
                state.bloggers.forEach { blogger ->
                    FilterChip(
                        selected = state.selectedBloggerId == blogger.id,
                        onClick = { onBloggerSelected(blogger.id) },
                        label = { Text(blogger.nickname) },
                        shape = RoundedCornerShape(6.dp),
                        colors = filterChipColors,
                    )
                }
            }

            state.selectedBloggerId?.let { bloggerId ->
                FilledTonalButton(
                    onClick = { onOpenBlogger(bloggerId) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Text(
                        text = stringResource(R.string.blogger_open_page),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.blogger_filters_done))
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(text: String) {
    Text(text = text, fontWeight = FontWeight.SemiBold)
}

private fun BloggerVideoSort.labelRes() = when (this) {
    BloggerVideoSort.NEW -> R.string.blogger_videos_sort_new
    BloggerVideoSort.TOP -> R.string.blogger_videos_sort_top
    BloggerVideoSort.OLD -> R.string.blogger_videos_sort_old
}
