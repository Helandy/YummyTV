package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.core.designsystem.presenter.components.toRatingColor
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.mobile.details.model.RatingLabel
import su.afk.yummy.tv.feature.details.mobile.details.utils.formatRating

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailsRatingRow(
    details: AnimeDetails,
    modifier: Modifier = Modifier,
) {
    val average = details.rating.average
    val externalLabels = buildList {
        details.rating.kinopoisk?.let {
            add(
                RatingLabel(
                    label = stringResource(
                        R.string.details_mobile_kinopoisk_rating,
                        it.formatRating()
                    ),
                    isPrimary = false,
                    rating = it,
                ),
            )
        }
        details.rating.shikimori?.let {
            add(
                RatingLabel(
                    stringResource(
                        R.string.details_mobile_shikimori_rating,
                        it.formatRating(),
                    ),
                    false,
                    it,
                ),
            )
        }
        details.rating.myAnimeList?.let {
            add(
                RatingLabel(
                    stringResource(
                        R.string.details_mobile_mal_rating,
                        it.formatRating(),
                    ),
                    false,
                    it,
                ),
            )
        }
    }
    if (average == null && externalLabels.isEmpty()) return

    var showExternalRatings by remember(externalLabels) { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DetailsAverageRating(
            rating = average,
            expandable = externalLabels.isNotEmpty(),
            onClick = { showExternalRatings = !showExternalRatings },
        )
        AnimatedVisibility(visible = showExternalRatings && externalLabels.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                externalLabels.forEach { item ->
                    DetailsRatingChip(item)
                }
            }
        }
    }
}

@Composable
private fun DetailsAverageRating(
    rating: Double?,
    expandable: Boolean,
    onClick: () -> Unit,
) {
    val color = rating?.toRatingColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = expandable, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = color ?: Color(0xFFFF8A00),
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = rating?.formatRating() ?: stringResource(R.string.details_mobile_rating),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun DetailsRatingChip(item: RatingLabel) {
    val color = item.rating?.toRatingColor() ?: Color.White
    Row(
        modifier = Modifier
            .background(
                color = Color.Black.copy(alpha = 0.46f),
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
        )
    }
}
