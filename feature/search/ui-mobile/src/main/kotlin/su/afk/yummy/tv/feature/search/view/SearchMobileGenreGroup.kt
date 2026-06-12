package su.afk.yummy.tv.feature.search.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.search.model.SearchGenre

@Composable
internal fun SearchMobileGenreGroup(
    title: String,
    genres: List<SearchGenre>,
    selectedIds: Set<String>,
    onGenreToggled: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChipFlow {
            genres.forEach { genre ->
                FilterChip(
                    label = genre.title,
                    selected = genre.id in selectedIds,
                    onClick = { onGenreToggled(genre.id) },
                )
            }
        }
    }
}
