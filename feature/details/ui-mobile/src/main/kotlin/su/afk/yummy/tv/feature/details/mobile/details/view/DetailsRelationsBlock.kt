package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.presentation.R

/** Студии, создатели и жанры; названия с id кликабельны и ведут в каталог. */
@Composable
internal fun DetailsRelationsBlock(
    details: AnimeDetails,
    onGenreSelected: (Int) -> Unit,
    onStudioSelected: (id: Int, url: String?) -> Unit,
    onDirectorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RelationRow(
            icon = Icons.Outlined.Business,
            groups = listOf(
                RelationGroup(
                    label = stringResource(R.string.details_mobile_studios),
                    items = details.studios.map { studio ->
                        Triple(studio.title, studio.id) { studio.id?.let { onStudioSelected(it, studio.url) } }
                    },
                ),
                RelationGroup(
                    label = stringResource(R.string.details_mobile_creators),
                    items = details.creators.map { creator ->
                        Triple(creator.title, creator.id) { creator.id?.let(onDirectorSelected) }
                    },
                ),
            ),
        )
        RelationRow(
            icon = Icons.Outlined.Category,
            groups = listOf(
                RelationGroup(
                    label = stringResource(R.string.details_full_genres),
                    items = details.genres.map { genre ->
                        Triple(genre.title, genre.id) { genre.id?.let(onGenreSelected) }
                    },
                ),
            ),
        )
    }
}

private class RelationGroup(
    val label: String,
    val items: List<Triple<String, Int?, () -> Unit>>,
)

/** Строка «иконка Подпись: элемент, элемент Подпись: …»; элементы с id кликабельны. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelationRow(
    icon: ImageVector,
    groups: List<RelationGroup>,
) {
    val visible = groups
        .map { group -> RelationGroup(group.label, group.items.filter { it.first.isNotBlank() }) }
        .filter { it.items.isNotEmpty() }
    if (visible.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .padding(top = 2.dp)
                .size(22.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            visible.forEachIndexed { groupIndex, group ->
                Text(
                    text = "${group.label}:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                group.items.forEachIndexed { index, (title, id, onClick) ->
                    val separated = index < group.items.lastIndex || groupIndex < visible.lastIndex
                    Text(
                        text = if (separated) "$title," else title,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (id != null) TextDecoration.Underline else null,
                        modifier = if (id != null) Modifier.clickable(onClick = onClick) else Modifier,
                    )
                }
            }
        }
    }
}
