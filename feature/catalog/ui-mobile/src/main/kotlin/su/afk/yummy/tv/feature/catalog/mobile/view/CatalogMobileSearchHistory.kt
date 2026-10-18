package su.afk.yummy.tv.feature.catalog.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import su.afk.yummy.tv.feature.catalog.presentation.R

/** Последние запросы чипами в горизонтальной ленте под пустым полем поиска и кнопка очистки справа. */
@Composable
internal fun CatalogMobileSearchHistory(
    queries: ImmutableList<String>,
    onQuerySelected: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(queries, key = { it }) { query ->
                SuggestionChip(
                    onClick = { onQuerySelected(query) },
                    label = { Text(query, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }
        TextButton(onClick = onClear) {
            Text(stringResource(R.string.search_mobile_history_clear))
        }
    }
}
