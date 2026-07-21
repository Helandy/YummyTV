package su.afk.yummy.tv.feature.details.subscriptions

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.feature.details.details.view.SubscriptionsPickerOverlay

@Composable
fun SubscriptionsTvScreen(
    state: SubscriptionsState.State,
    effect: Flow<SubscriptionsState.Effect>,
    onEvent: (SubscriptionsState.Event) -> Unit,
) {
    SubscriptionsPickerOverlay(
        subscriptions = state.subscriptions,
        isLoading = state.isLoading,
        onToggle = { key -> onEvent(SubscriptionsState.Event.SubscriptionToggled(key)) },
        onDismiss = { onEvent(SubscriptionsState.Event.BackSelected) },
    )
}
