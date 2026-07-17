package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.mobile.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailsSecondaryActions(
    state: DetailsState.State,
    details: AnimeDetails,
    onSubscriptionsSelected: () -> Unit,
    onFullDetailsSelected: () -> Unit,
    onEpisodesSelected: () -> Unit,
    onTrailersSelected: () -> Unit,
    onSimilarSelected: () -> Unit,
    onViewingOrderSelected: () -> Unit,
    onScreenshotsSelected: () -> Unit,
    onRatingScreenSelected: () -> Unit,
    onCollectionsSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = buildMobileActions(
        state = state,
        details = details,
        onSubscriptionsSelected = onSubscriptionsSelected,
        onFullDetailsSelected = onFullDetailsSelected,
        onEpisodesSelected = onEpisodesSelected,
        onTrailersSelected = onTrailersSelected,
        onSimilarSelected = onSimilarSelected,
        onViewingOrderSelected = onViewingOrderSelected,
        onScreenshotsSelected = onScreenshotsSelected,
        onRatingScreenSelected = onRatingScreenSelected,
        onCollectionsSelected = onCollectionsSelected,
    )
    if (actions.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.details_mobile_sections),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            actions.forEach { action ->
                DetailsActionCard(
                    action = action,
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                )
            }
            if (actions.size % 2 != 0) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                )
            }
        }
    }
}
