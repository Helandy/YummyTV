package su.afk.yummy.tv.feature.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.AnimeRatingBucket
import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import su.afk.yummy.tv.feature.details.R

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

@Composable
private fun RatingDistribution(distribution: List<AnimeRatingBucket>) {
    val total = distribution.sumOf { it.count }
    if (total <= 0) {
        Text(
            text = stringResource(R.string.details_rating_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.details_rating_votes_short, total),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        distribution.sortedByDescending { it.rating }.forEach { bucket ->
            val fraction = bucket.count.toFloat() / total.toFloat()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = bucket.rating.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.width(24.dp),
                )
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .height(10.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), RoundedCornerShape(5.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .height(10.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp)),
                    )
                }
                Text(
                    text = bucket.count.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ListStatsRow(listStats: AnimeListStats) {
    val watching = listStats.counts[0] ?: 0
    val planned = listStats.counts[1] ?: 0
    val completed = listStats.counts[2] ?: 0
    if (watching + planned + completed <= 0) return
    RatingSummaryPill(stringResource(R.string.details_list_stats, watching, planned, completed))
}

@Composable
private fun RatingAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    focusRequester: FocusRequester? = null,
    compact: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        onClick()
                        true
                    }
                    else -> false
                }
            }
            .tvFocusableClick(onClick = onClick, shape = shape)
            .background(background, shape)
            .padding(horizontal = if (compact) 8.dp else 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon?.let {
            it()
            if (!compact) androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun RatingSummaryPill(label: String) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
