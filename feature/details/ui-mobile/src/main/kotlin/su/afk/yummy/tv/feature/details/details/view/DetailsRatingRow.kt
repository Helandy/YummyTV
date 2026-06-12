package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.components.toRatingColor
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.feature.details.details.model.RatingLabel
import su.afk.yummy.tv.feature.details.details.utils.formatRating
import su.afk.yummy.tv.feature.details.mobile.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailsRatingRow(
    details: AnimeDetails,
    modifier: Modifier = Modifier,
) {
    val labels = buildList {
        details.rating.average?.let {
            add(RatingLabel(it.formatRating(), true, it))
        }
        details.rating.kinopoisk?.let {
            add(RatingLabel(stringResource(R.string.details_mobile_kinopoisk_rating, it.formatRating()), false))
        }
        details.rating.shikimori?.let { add(RatingLabel("Shikimori ${it.formatRating()}", false)) }
        details.rating.myAnimeList?.let { add(RatingLabel("MAL ${it.formatRating()}", false)) }
    }
    if (labels.isEmpty()) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEach { item ->
            val primaryColor = item.rating?.toRatingColor()
            Row(
                modifier = Modifier
                    .background(
                        color = if (item.isPrimary) Color.Black.copy(alpha = 0.62f) else Color.Black.copy(
                            alpha = 0.46f
                        ),
                        shape = RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = primaryColor ?: Color(0xFFFFC857),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (item.isPrimary) FontWeight.ExtraBold else FontWeight.Bold,
                    color = primaryColor ?: Color.White,
                    maxLines = 1,
                )
            }
        }
    }
}
