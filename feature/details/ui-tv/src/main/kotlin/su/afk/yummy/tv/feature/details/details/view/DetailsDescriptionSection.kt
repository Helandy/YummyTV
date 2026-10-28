package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.presentation.R

/** Описание с раскрытием по OK, жанры, студии и создатели как фокусируемые чипы. */
@Composable
internal fun DetailsDescriptionSection(
    details: AnimeDetails,
    onGenreSelected: (Int) -> Unit,
    onStudioSelected: (id: Int, url: String?) -> Unit,
    onDirectorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    descriptionFocusModifier: Modifier = Modifier,
    chipFocusModifier: @Composable (index: Int) -> Modifier = { Modifier },
) {
    val chips = buildList {
        details.genres.forEach { genre ->
            genre.id?.let { id -> add(DetailsChipData(genre.title, null) { onGenreSelected(id) }) }
        }
        details.studios.forEach { studio ->
            studio.id?.let { id ->
                add(DetailsChipData(studio.title, Icons.Outlined.Business) { onStudioSelected(id, studio.url) })
            }
        }
        details.creators.forEach { creator ->
            creator.id?.let { id ->
                add(DetailsChipData(creator.title, Icons.Outlined.Person) { onDirectorSelected(id) })
            }
        }
    }.filter { it.title.isNotBlank() }
    if (chips.isEmpty() && details.description.isBlank()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (details.description.isNotBlank()) {
            var expanded by rememberSaveable { mutableStateOf(false) }
            var overflows by remember { mutableStateOf(false) }
            val shape = RoundedCornerShape(8.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(descriptionFocusModifier)
                    .tvFocusableClick(onClick = { expanded = !expanded }, shape = shape, focusedScale = 1.01f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = details.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
                )
                if (overflows || expanded) {
                    Text(
                        text = stringResource(
                            if (expanded) R.string.details_mobile_less else R.string.details_mobile_more,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (chips.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEachIndexed { index, chip ->
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        modifier = Modifier
                            .then(chipFocusModifier(index))
                            .tvFocusableClick(onClick = chip.onClick, shape = shape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), shape)
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        chip.icon?.let {
                            Icon(
                                imageVector = it,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Text(
                            text = chip.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

private data class DetailsChipData(
    val title: String,
    val icon: ImageVector?,
    val onClick: () -> Unit,
)
