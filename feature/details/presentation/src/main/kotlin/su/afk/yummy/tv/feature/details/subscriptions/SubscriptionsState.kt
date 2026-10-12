package su.afk.yummy.tv.feature.details.subscriptions

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.feature.details.details.model.SubscriptionOption

class SubscriptionsState {
    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val error: UiText? = null,
        val subscriptions: ImmutableList<SubscriptionOption> = persistentListOf(),
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
