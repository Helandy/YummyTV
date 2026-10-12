package su.afk.yummy.tv.feature.details.full

import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.core.model.anime.AnimeDetails

class FullDetailsState {
    data class State(
        val isLoading: Boolean = true,
        val details: AnimeDetails? = null,
        val error: UiText? = null,
    ) : UiState

    /** Пользовательские действия на экране полного описания. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь запросил повторную загрузку описания. */
        data object RetrySelected : Event

        data class GenreSelected(val id: Int) : Event
        data class StudioSelected(val id: Int, val url: String?) : Event
        data class DirectorSelected(val id: Int) : Event
    }

    sealed interface Effect : UiEffect
}
