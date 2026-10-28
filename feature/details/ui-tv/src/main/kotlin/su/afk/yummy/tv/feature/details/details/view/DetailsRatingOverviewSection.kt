package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.scoreDistributionColor
import su.afk.yummy.tv.core.designsystem.components.toRatingTextColor
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.core.utils.formatting.formatRating
import su.afk.yummy.tv.core.utils.formatting.toGroupedCount
import su.afk.yummy.tv.feature.details.details.model.DetailsRatingOverview
import su.afk.yummy.tv.feature.details.details.utils.label
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.utils.statusColor
import su.afk.yummy.tv.feature.details.utils.statusIcon

/**
 * Обзор рейтинга: средняя оценка, гистограмма голосов и доли списков. Клик открывает экран оценки.
 * Без данных ([overview] == null) секция остаётся — это единственный путь поставить первую оценку.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailsRatingOverviewSection(
    overview: DetailsRatingOverview?,
    userRating: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionFocus = rememberHeaderActionFocus()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DetailsSectionHeader(
            title = stringResource(R.string.details_rating_button),
            actionFocus = actionFocus,
            modifier = Modifier.padding(horizontal = DetailsSectionHorizontalPadding),
            // Без оценки — приглашение оценить, с оценкой — своя оценка; оба варианта открывают экран оценки.
            actionLabel = userRating?.let { stringResource(R.string.details_your_rating, it.toString()) }
                ?: stringResource(R.string.details_mobile_rate_action),
            onActionClick = onClick,
        )
        Column(
            modifier = Modifier
                .padding(horizontal = DetailsSectionHorizontalPadding, vertical = 4.dp)
                .fillMaxWidth()
                .then(actionFocus.opensOn(listOf(Key.DirectionRight)))
                .tvFocusableClick(onClick = onClick, shape = RoundedCornerShape(12.dp), focusedScale = 1.01f)
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                if (overview != null && overview.votes > 0) {
                    RatingAverage(
                        average = overview.average,
                        votes = overview.votes,
                        modifier = Modifier.width(220.dp),
                    )
                    RatingHistogram(
                        distribution = overview.distribution,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.details_mobile_rating_not_set),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (overview != null && overview.listCounts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    overview.listCounts.forEach { item ->
                        Box(
                            modifier = Modifier
                                .weight(item.count.toFloat())
                                .fillMaxHeight()
                                .background(item.list.statusColor()),
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    overview.listCounts.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = item.list.statusIcon(),
                                contentDescription = null,
                                tint = item.list.statusColor(),
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = item.list.label(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = item.count.toGroupedCount(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingAverage(
    average: Double?,
    votes: Int,
    modifier: Modifier = Modifier,
) {
    val color = average?.toRatingTextColor() ?: MaterialTheme.colorScheme.onSurface
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = average?.formatRating() ?: "-",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1,
            )
        }
        Text(
            text = stringResource(R.string.details_full_rating_votes, votes.toGroupedCount()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Мини-гистограмма голосов за оценки 1..10: индекс = оценка − 1. */
@Composable
private fun RatingHistogram(
    distribution: List<Int>,
    modifier: Modifier = Modifier,
) {
    val max = distribution.maxOrNull()?.takeIf { it > 0 } ?: return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        distribution.forEachIndexed { index, count ->
            val fraction = (count.toFloat() / max).coerceAtLeast(0.06f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction)
                    .background(
                        color = scoreDistributionColor(index + 1)
                            .copy(alpha = if (count > 0) 0.9f else 0.25f),
                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                    ),
            )
        }
    }
}
