package su.afk.yummy.tv.feature.home.launch

import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState

class ContinueWatchingLaunchState {
    /** Экран живёт только пока разрешается запуск — показывает лоадер. */
    data object State : UiState

    sealed interface Event : UiEvent

    sealed interface Effect : UiEffect
}
