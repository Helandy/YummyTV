package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.presentation.R

/**
 * Жанры, студии, создатели и сокращённое описание под кнопками.
 * Длинное описание разворачивается по «Подробнее…»; другие названия — в шторке по (i).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailsInfoBlock(
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
            icon = Icons.Outlined.Category,
            label = stringResource(R.string.details_full_genres),
            items = details.genres.map { genre -> Triple(genre.title, genre.id) { genre.id?.let(onGenreSelected) } },
        )
        RelationRow(
            icon = Icons.Outlined.Business,
            label = stringResource(R.string.details_mobile_studios),
            items = details.studios.map { studio ->
                Triple(studio.title, studio.id) { studio.id?.let { onStudioSelected(it, studio.url) } }
            },
        )
        RelationRow(
            icon = Icons.Outlined.Groups,
            label = stringResource(R.string.details_mobile_creators),
            items = details.creators.map { creator ->
                Triple(creator.title, creator.id) { creator.id?.let(onDirectorSelected) }
            },
        )
        if (details.description.isNotBlank()) {
            var expanded by rememberSaveable { mutableStateOf(false) }
            var overflows by remember { mutableStateOf(false) }
            Text(
                text = details.description,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
            )
            if (overflows || expanded) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text = stringResource(if (expanded) R.string.details_mobile_less else R.string.details_mobile_more),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** Строка «иконка Подпись: элемент, элемент»; элементы с id кликабельны. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelationRow(
    icon: ImageVector,
    label: String,
    items: List<Triple<String, Int?, () -> Unit>>,
) {
    val visible = items.filter { it.first.isNotBlank() }
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
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            visible.forEachIndexed { index, (title, id, onClick) ->
                Text(
                    text = if (index < visible.lastIndex) "$title," else title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (id != null) TextDecoration.Underline else null,
                    modifier = if (id != null) Modifier.clickable(onClick = onClick) else Modifier,
                )
            }
        }
    }
}
