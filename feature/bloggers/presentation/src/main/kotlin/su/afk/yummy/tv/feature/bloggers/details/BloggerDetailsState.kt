package su.afk.yummy.tv.feature.bloggers.details

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.bloggers.model.BloggerDetails
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo

class BloggerDetailsState {
    @Immutable
    data class State(
        val blogger: BloggerDetails? = null,
        val videos: ImmutableList<BloggerVideo> = persistentListOf(),
        val currentUserId: Int = 0,
        val loading: Boolean = true,
        val subscribing: Boolean = false,
        val error: UiText? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
        data object SubscribeSelected : Event
        data class VideoSelected(val videoId: Int) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
    }
}
