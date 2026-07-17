package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.details.utils.formatViews

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailsMetaChips(details: AnimeDetails) {
    val chips = buildList {
        details.year?.let { add(it.toString()) }
        details.type?.takeIf { it.isNotBlank() }?.let { add(it) }
        details.status?.takeIf { it.isNotBlank() }?.let { add(it) }
        details.ageRating?.takeIf { it.isNotBlank() }?.let { add(it) }
    }
    if (chips.isEmpty() && details.views == null) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        chips.forEach { chip ->
            DetailsChip(label = chip)
        }
        details.views?.let { views ->
            DetailsChip(
                label = views.formatViews(),
                icon = Icons.Filled.Visibility,
            )
        }
    }
}
