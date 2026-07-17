package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.runtime.Composable
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.details.DetailsState

@Composable
internal fun DetailsPickerSheets(
    state: DetailsState.State,
    onLibraryListSelected: (UserAnimeList) -> Unit,
    onLibraryDismiss: () -> Unit,
    onSubscriptionToggle: (String) -> Unit,
    onSubscriptionsDismiss: () -> Unit,
    onBalancerConfirmed: (su.afk.yummy.tv.core.model.anime.AnimeVideo) -> Unit,
    onBalancerDismiss: () -> Unit,
) {
    if (state.showLibraryListPicker) {
        LibraryListDialog(
            onSelected = onLibraryListSelected,
            onDismiss = onLibraryDismiss,
        )
    }
    if (state.showSubscriptionsPicker) {
        SubscriptionsDialog(
            state = state,
            onToggle = onSubscriptionToggle,
            onDismiss = onSubscriptionsDismiss,
        )
    }
    state.pendingBalancerSelection?.let { picker ->
        BalancerDialog(
            picker = picker,
            onConfirmed = onBalancerConfirmed,
            onDismiss = onBalancerDismiss,
        )
    }
}
