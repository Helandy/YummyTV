package su.afk.yummy.tv.feature.library.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.LibrarySort
import su.afk.yummy.tv.core.model.settings.LibrarySortDirection
import su.afk.yummy.tv.feature.library.mobile.utils.mobileLabel
import su.afk.yummy.tv.feature.library.presentation.R

/** Порядок вариантов сортировки в меню. */
private val librarySortOrder = listOf(
    LibrarySort.ADDED_DATE,
    LibrarySort.YEAR,
    LibrarySort.RATING,
    LibrarySort.USER_RATING,
    LibrarySort.TITLE,
)

/**
 * Выбор сортировки списков библиотеки: чипа с выпадающим меню вариантов и отдельная чипа-стрелка,
 * разворачивающая порядок.
 */
@Composable
internal fun LibraryMobileSortRow(
    sort: LibrarySort,
    direction: LibrarySortDirection,
    onSortSelected: (LibrarySort) -> Unit,
    onDirectionToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            FilterChip(
                selected = false,
                onClick = { isMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        text = stringResource(
                            R.string.library_mobile_sort_label,
                            sort.mobileLabel()
                        )
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )

            // Якорь нулевой ширины у правого края чипы: меню не помещается вправо от него и
            // раскладывается влево, то есть прижимается к правому краю контрола.
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false },
                ) {
                    librarySortOrder.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option.mobileLabel()) },
                            onClick = {
                                isMenuExpanded = false
                                onSortSelected(option)
                            },
                            trailingIcon = if (option == sort) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }

        FilterChip(
            selected = false,
            onClick = onDirectionToggled,
            label = {
                Icon(
                    imageVector = when (direction) {
                        LibrarySortDirection.DESC -> Icons.Default.KeyboardArrowDown
                        LibrarySortDirection.ASC -> Icons.Default.KeyboardArrowUp
                    },
                    contentDescription = stringResource(
                        R.string.library_mobile_sort_direction_content_description,
                    ),
                    modifier = Modifier.size(18.dp),
                )
            },
        )
    }
}
