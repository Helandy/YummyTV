package su.afk.yummy.tv.feature.bloggers.details

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.bloggers.model.BloggerDetails
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo

object BloggerDetailsState {
    data class State(
        val blogger: BloggerDetails? = null,
        val videos: List<BloggerVideo> = emptyList(),
        val currentUserId: Int = 0,
        val loading: Boolean = true,
        val subscribing: Boolean = false,
        val error: String? = null,
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
