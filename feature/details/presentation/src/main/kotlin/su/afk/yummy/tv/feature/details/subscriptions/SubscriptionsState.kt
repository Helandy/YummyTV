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

    /** Пользовательские действия на экране подписок тайтла. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь запросил повторную загрузку подписок. */
        data object RetrySelected : Event

        /** Пользователь переключил подписку с указанным ключом. */
        data class SubscriptionToggled(val key: String) : Event
    }

    sealed interface Effect : UiEffect
}
