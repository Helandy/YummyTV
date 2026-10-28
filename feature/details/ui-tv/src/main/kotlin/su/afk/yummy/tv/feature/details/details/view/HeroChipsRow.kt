package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.core.model.anime.AnimeDetails

/** Жанры, студии и создатели как фокусируемые чипы в одну строку с прокруткой. */
@Composable
internal fun HeroChipsRow(
    details: AnimeDetails,
    onGenreSelected: (Int) -> Unit,
    onStudioSelected: (id: Int, url: String?) -> Unit,
    onDirectorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    chipFocusModifier: @Composable (index: Int) -> Modifier = { Modifier },
) {
    val chips = buildList {
        details.genres.forEach { genre ->
            genre.id?.let { id -> add(HeroChipData(genre.title, null) { onGenreSelected(id) }) }
        }
        details.studios.forEach { studio ->
            studio.id?.let { id ->
                add(HeroChipData(studio.title, Icons.Outlined.Business) { onStudioSelected(id, studio.url) })
            }
        }
        details.creators.forEach { creator ->
            creator.id?.let { id ->
                add(HeroChipData(creator.title, Icons.Outlined.Person) { onDirectorSelected(id) })
            }
        }
    }.filter { it.title.isNotBlank() }
    if (chips.isEmpty()) return

    Row(
        modifier = modifier
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

private data class HeroChipData(
    val title: String,
    val icon: ImageVector?,
    val onClick: () -> Unit,
)
