package su.afk.yummy.tv.feature.home.recommendations

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.home.model.HomeFeedItem

class HomeRecommendationsState {
    @Immutable
    data class State(
        /** Заголовок секции из ленты; пуст, пока лента не прочитана. */
        val title: String = "",
        /** Рекомендации без скрытых пользователем тайтлов. */
        val items: ImmutableList<HomeFeedItem> = persistentListOf(),
        val isLoading: Boolean = true,
    ) : UiState

    /** Пользовательские действия на экране рекомендаций. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь выбрал аниме с указанным идентификатором. */
        data class AnimeSelected(val animeId: Int) : Event
    }

    sealed interface Effect : UiEffect
}
