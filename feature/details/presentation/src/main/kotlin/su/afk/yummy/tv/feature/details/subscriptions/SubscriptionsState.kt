package su.afk.yummy.tv.feature.details.subscriptions

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.feature.details.details.SubscriptionOption

class SubscriptionsState {
    data class State(
        val isLoading: Boolean = true,
        val error: String? = null,
        val subscriptions: List<SubscriptionOption> = emptyList(),
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
        data class SubscriptionToggled(val key: String) : Event
    }

    sealed interface Effect : UiEffect
}
