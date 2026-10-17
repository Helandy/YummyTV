package su.afk.yummy.tv.feature.details.trailers

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.core.model.anime.AnimeTrailer

class TrailersState {
    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val trailers: ImmutableList<AnimeTrailer> = persistentListOf(),
        val error: UiText? = null,
    ) : UiState

    /** Пользовательские действия на экране трейлеров. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь запросил повторную загрузку трейлеров. */
        data object RetrySelected : Event

        /** Пользователь выбрал трейлер для просмотра в приложении. */
        data class TrailerSelected(val trailer: AnimeTrailer) : Event
    }

    sealed interface Effect : UiEffect
}
