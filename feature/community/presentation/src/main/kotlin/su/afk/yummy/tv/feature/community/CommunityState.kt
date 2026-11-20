package su.afk.yummy.tv.feature.community

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.domain.posts.model.PostSummary
import su.afk.yummy.tv.feature.community.model.CommunitySection

class CommunityState {
    data class State(
        val news: ImmutableList<PostSummary> = persistentListOf(),
        val isNewsLoading: Boolean = true,
        val newsError: UiText? = null,
        val videos: ImmutableList<BloggerVideo> = persistentListOf(),
        val isVideosLoading: Boolean = true,
        val videosError: UiText? = null,
        val isRefreshing: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class SectionSelected(val section: CommunitySection) : Event
        data object AllNewsSelected : Event
        data class NewsSelected(val postId: Int) : Event
        data object AllVideosSelected : Event
        data class VideoSelected(val videoId: Int) : Event
        data object NewsRetrySelected : Event
        data object VideosRetrySelected : Event
        data object RefreshSelected : Event
    }

    sealed interface Effect : UiEffect
}
