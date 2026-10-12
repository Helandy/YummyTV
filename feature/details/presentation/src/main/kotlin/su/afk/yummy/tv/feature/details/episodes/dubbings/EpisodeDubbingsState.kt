package su.afk.yummy.tv.feature.details.episodes.dubbings

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.feature.details.details.model.BalancerPickerState

class EpisodeDubbingsState {
    @Immutable
    data class State(
        val episode: String = "",
        val isLoading: Boolean = true,
        val error: UiText? = null,
        val dubbings: ImmutableList<DubbingItem> = persistentListOf(),
        val pendingBalancerSelection: BalancerPickerState? = null,
    ) : UiState

    data class DubbingItem(
        val name: String,
        val views: Int,
        val episodeCount: Int,
        val supportedBalancers: String,
    )

    /** Пользовательские действия на экране озвучек эпизода. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь выбрал озвучку для запуска текущей серии. */
        data class DubbingSelected(val name: String) : Event

        /** Пользователь запросил повторную загрузку озвучек. */
        data object RetrySelected : Event

        /** Пользователь подтвердил видео для запуска после выбора балансера. */
        data class BalancerConfirmed(val video: AnimeVideo) : Event

        /** Пользователь закрыл выбор балансера. */
        data object BalancerPickerDismissed : Event
    }

    sealed interface Effect : UiEffect
}
