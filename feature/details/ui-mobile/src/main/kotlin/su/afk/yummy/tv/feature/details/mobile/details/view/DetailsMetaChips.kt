package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.common.anime.isReleasedAnimeStatus
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.mobile.details.utils.formatViews

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailsMetaChips(details: AnimeDetails) {
    val chips = buildList {
        details.year?.let { add(it.toString()) }
        details.type?.takeIf { it.isNotBlank() }?.let { add(it) }
    }
    val status = details.status?.takeIf { it.isNotBlank() }
    val ageRating = details.ageRating?.takeIf { it.isNotBlank() }
    if (chips.isEmpty() && details.views == null && status == null && ageRating == null) return

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        chips.forEach { chip ->
            DetailsChip(label = chip)
        }
        status?.let {
            DetailsChip(
                label = it,
                textColor = if (it.isReleasedAnimeStatus()) null else MaterialTheme.colorScheme.primary,
            )
        }
        details.views?.let { views ->
            DetailsChip(
                label = views.formatViews(),
                icon = Icons.Filled.Visibility,
            )
        }
        ageRating?.let {
            DetailsChip(label = it, textColor = Color.White, backgroundColor = Color.Black)
        }
    }
}
