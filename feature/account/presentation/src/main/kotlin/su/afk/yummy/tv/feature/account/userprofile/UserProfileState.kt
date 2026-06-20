package su.afk.yummy.tv.feature.account.userprofile

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.model.UserFriend
import su.afk.yummy.tv.domain.account.model.UserPostSummary
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserReviewSummary
import su.afk.yummy.tv.domain.account.model.UserStats

class UserProfileState {
    data class State(
        val userId: Int = 0,
        val selectedTab: Tab = Tab.OVERVIEW,
        val profile: UserProfileSummary? = null,
        val stats: UserStats? = null,
        val isOverviewLoading: Boolean = true,
        val overviewError: Boolean = false,
        val selectedList: ListFilter = ListFilter.WATCHING,
        val lists: PagedContent<UserAnimeListItem> = PagedContent(),
        val collections: Flow<PagingData<AnimeCollectionSummary>> = flowOf(PagingData.empty()),
        val posts: Flow<PagingData<UserPostSummary>> = flowOf(PagingData.empty()),
        val reviews: Flow<PagingData<UserReviewSummary>> = flowOf(PagingData.empty()),
        val friends: Flow<PagingData<UserFriend>> = flowOf(PagingData.empty()),
    ) : UiState

    data class PagedContent<T>(
        val items: List<T> = emptyList(),
        val isLoading: Boolean = false,
        val error: Boolean = false,
        val loaded: Boolean = false,
    )

    enum class Tab {
        OVERVIEW,
        LISTS,
        COLLECTIONS,
        POSTS,
        REVIEWS,
        FRIENDS,
    }

    enum class ListFilter(val list: UserAnimeList?) {
        WATCHING(UserAnimeList.WATCHING),
        PLANNED(UserAnimeList.PLANNED),
        COMPLETED(UserAnimeList.COMPLETED),
        DROPPED(UserAnimeList.DROPPED),
        POSTPONED(UserAnimeList.POSTPONED),
        FAVORITES(null),
    }

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetryOverviewSelected : Event
        data class TabSelected(val tab: Tab) : Event
        data class ListFilterSelected(val filter: ListFilter) : Event
        data object RetryTabSelected : Event
        data class AnimeSelected(val animeId: Int) : Event
        data class FriendSelected(val userId: Int) : Event
    }

    sealed interface Effect : UiEffect
}
