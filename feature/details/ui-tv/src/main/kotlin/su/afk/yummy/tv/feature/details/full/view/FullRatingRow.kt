package su.afk.yummy.tv.feature.details.full.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.utils.formatRating
import su.afk.yummy.tv.feature.details.utils.formatViews

@Composable
internal fun FullRatingRow(details: AnimeDetails) {
    val ratings = buildList {
        details.views?.let { add(stringResource(R.string.details_full_views, it.formatViews())) }
        details.rating.counters?.let { add(stringResource(R.string.details_full_rating_votes, it)) }
        details.rating.myAnimeList?.let {
            add(
                stringResource(
                    R.string.details_mal_rating,
                    it.formatRating()
                )
            )
        }
        details.rating.kinopoisk?.let { add(stringResource(R.string.details_kinopoisk_rating, it.formatRating())) }
        details.rating.shikimori?.let {
            add(
                stringResource(
                    R.string.details_shikimori_rating,
                    it.formatRating()
                )
            )
        }
    }
    if (details.rating.average == null && ratings.isEmpty()) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        details.rating.average?.let { rating ->
            FullYaniRatingLabel(rating)
        }
        ratings.forEach { rating ->
            Text(
                text = rating,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
            )
        }
    }
}
