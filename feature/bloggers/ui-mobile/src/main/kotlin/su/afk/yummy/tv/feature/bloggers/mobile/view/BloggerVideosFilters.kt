package su.afk.yummy.tv.feature.bloggers.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheet
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTextTabs
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoSort
import su.afk.yummy.tv.feature.bloggers.list.BloggerVideosListState
import su.afk.yummy.tv.feature.bloggers.presentation.R
import su.afk.yummy.tv.feature.bloggers.utils.labelRes
import su.afk.yummy.tv.core.designsystem.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BloggerVideosFilters(
    state: BloggerVideosListState.State,
    onCategorySelected: (String) -> Unit,
    onBloggerSelected: (Int?) -> Unit,
    onSortSelected: (BloggerVideoSort) -> Unit,
    onOpenBlogger: (Int) -> Unit,
    onReset: () -> Unit,
    showFilters: Boolean,
    onFiltersDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCategory = state.categories.firstOrNull { it.id == state.selectedCategory }
    val selectedBlogger = state.bloggers.firstOrNull { it.id == state.selectedBloggerId }
    val activeFilters = listOfNotNull(selectedCategory?.title, selectedBlogger?.nickname)
    Column(modifier = modifier) {
        val sorts = BloggerVideoSort.entries
        MobileTextTabs(
            titles = sorts.map { stringResource(it.labelRes()) },
            selectedIndex = sorts.indexOf(state.sort).coerceAtLeast(0),
            onSelected = { onSortSelected(sorts[it]) },
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
        )

        if (activeFilters.isNotEmpty()) {
            Text(
                text = activeFilters.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (showFilters) {
        BloggerVideosFiltersSheet(
            state = state,
            onDismiss = onFiltersDismiss,
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

    BaseBottomSheet(
        onDismissRequest = onDismiss,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
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
                    fontWeight = FontWeight.SemiBold,
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
            FilterSectionTitle(stringResource(R.string.blogger_details_title))
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
                Text(stringResource(CoreR.string.common_done))
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(text: String) {
    Text(text = text, fontWeight = FontWeight.SemiBold)
}
