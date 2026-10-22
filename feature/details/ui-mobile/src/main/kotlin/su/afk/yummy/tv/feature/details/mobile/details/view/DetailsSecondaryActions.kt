package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.details.details.DetailsState

@Composable
internal fun DetailsSecondaryActions(
    state: DetailsState.State,
    onSubscriptionsSelected: () -> Unit,
    onSubscriptionsLockedSelected: () -> Unit,
    onEpisodesSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = buildMobileActions(
        state = state,
        onSubscriptionsSelected = onSubscriptionsSelected,
        onSubscriptionsLockedSelected = onSubscriptionsLockedSelected,
        onEpisodesSelected = onEpisodesSelected,
    )
    if (actions.isEmpty()) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        actions.forEach { action ->
            DetailsActionCard(
                action = action,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
