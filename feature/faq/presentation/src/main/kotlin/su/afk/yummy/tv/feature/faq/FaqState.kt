package su.afk.yummy.tv.feature.faq

import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState

class FaqState {
    /** Тексты FAQ статичны и лежат в ресурсах экрана, поэтому состояние пустое. */
    data object State : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
    }

    sealed interface Effect : UiEffect
}
