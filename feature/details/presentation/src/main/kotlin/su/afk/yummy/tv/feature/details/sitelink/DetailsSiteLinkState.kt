package su.afk.yummy.tv.feature.details.sitelink

import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState

class DetailsSiteLinkState {
    /** Пока slug разрешается — лоадер; не удалось — сообщение с повтором. */
    data class State(val isFailed: Boolean = false) : UiState

    sealed interface Event : UiEvent {
        data object RetrySelected : Event

        data object BackSelected : Event
    }

    sealed interface Effect : UiEffect
}
