package su.afk.yummy.tv.feature.details.rating

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.rating.view.ListStatsRow
import su.afk.yummy.tv.feature.details.rating.view.RatingAction
import su.afk.yummy.tv.feature.details.rating.view.RatingDistribution
import su.afk.yummy.tv.feature.details.rating.view.RatingSummaryPill

@Composable
internal fun DetailsRatingScreen(

    ratingSummary: AnimeRatingSummary,
    listStats: AnimeListStats,
    selectedUserRating: Int?,
    onRatingSelected: (Int) -> Unit,
    onRatingDeleted: () -> Unit,

) {
    val ratingFocusRequesters = remember { List(10) { FocusRequester() } }
    LaunchedEffect(selectedUserRating) {
        val focusIndex = ((selectedUserRating ?: 10) - 1).coerceIn(0, 9)
        runCatching { ratingFocusRequesters[focusIndex].requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
            .padding(horizontal = TvScreenPadding.Horizontal, vertical = TvScreenPadding.Vertical),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.78f)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(
                text = stringResource(R.string.details_rating_screen_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                selectedUserRating?.let { rating ->
                    RatingSummaryPill(stringResource(R.string.details_your_rating, rating))
                    RatingAction(label = stringResource(R.string.details_rating_delete), onClick = onRatingDeleted)
                } ?: RatingSummaryPill(stringResource(R.string.details_rating_not_set))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                (1..10).forEach { rating ->
                    RatingAction(
                        label = rating.toString(),
                        onClick = { onRatingSelected(rating) },
                        selected = selectedUserRating == rating,
                        focusRequester = ratingFocusRequesters[rating - 1],
                        modifier = Modifier.weight(1f),
                        compact = true,
                        icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                    )
                }
            }

            RatingDistribution(ratingSummary.distribution)
            ListStatsRow(listStats)
        }
    }
}
