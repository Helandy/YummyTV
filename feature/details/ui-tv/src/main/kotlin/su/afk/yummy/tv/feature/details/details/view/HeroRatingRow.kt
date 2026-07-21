package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.details.model.ExternalRatingLabel
import su.afk.yummy.tv.feature.details.utils.formatRating

@Composable
internal fun HeroRatingRow(details: AnimeDetails) {
    val yaniRating = details.rating.average
    val ratings = buildList {
        details.rating.kinopoisk?.let {
            add(
                ExternalRatingLabel(
                    label = stringResource(
                        R.string.details_kinopoisk_rating,
                        it.formatRating(),
                    ),
                    rating = it,
                )
            )
        }
        details.rating.shikimori?.let {
            add(
                ExternalRatingLabel(
                    label = stringResource(
                        R.string.details_shikimori_rating,
                        it.formatRating(),
                    ),
                    rating = it,
                )
            )
        }
        details.rating.myAnimeList?.let {
            add(
                ExternalRatingLabel(
                    label = stringResource(
                        R.string.details_mal_rating,
                        it.formatRating(),
                    ),
                    rating = it,
                )
            )
        }
    }
    if (yaniRating == null && ratings.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        yaniRating?.let { rating ->
            YaniRatingLabel(rating)
        }
        ratings.forEach { rating ->
            RatingLabel(rating)
        }
    }
}
