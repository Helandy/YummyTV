package su.afk.yummy.tv.feature.account.userprofile

import su.afk.yummy.tv.feature.account.userprofile.model.UserProfileCommentPreview
import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.FriendshipStatus
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.model.UserFriend
import su.afk.yummy.tv.domain.account.model.UserPostSummary
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserReviewSummary
import su.afk.yummy.tv.domain.account.model.UserStats

class UserProfileState {
    @Immutable
    data class State(
        val userId: Int = 0,
        val selectedTab: Tab = Tab.OVERVIEW,
        /** Вкладки, которые показываем: пока профиль не загружен — только обзор, потом без пустых. */
        val visibleTabs: ImmutableList<Tab> = persistentListOf(Tab.OVERVIEW),
        val profile: UserProfileSummary? = null,
        val stats: UserStats? = null,
        val isOverviewLoading: Boolean = true,
        val overviewError: Boolean = false,
        val selectedList: ListFilter = ListFilter.WATCHING,
        val lists: PagedContent<UserAnimeListItem> = PagedContent(),
        /** Пользователь закрыл списки приватностью — вместо фильтров показываем «Доступ запрещён». */
        val listsHidden: Boolean = false,
        /** Настройка «Отображать год у тайтлов в библиотеке» — действует и на списки в профиле. */
        val showTitleYear: Boolean = false,
        val comments: PagedContent<UserProfileCommentPreview> = PagedContent(),
        val collections: Flow<PagingData<AnimeCollectionSummary>> = flowOf(PagingData.empty()),
        val posts: Flow<PagingData<UserPostSummary>> = flowOf(PagingData.empty()),
        val reviews: Flow<PagingData<UserReviewSummary>> = flowOf(PagingData.empty()),
        val friends: Flow<PagingData<UserFriend>> = flowOf(PagingData.empty()),
        val isAuthorized: Boolean = false,
        val isOwnProfile: Boolean = false,
        val friendshipStatus: FriendshipStatus = FriendshipStatus.NONE,
        val isFriendshipLoading: Boolean = false,
        val friendshipError: Boolean = false,
    ) : UiState

    @Immutable
    data class PagedContent<T>(
        val items: ImmutableList<T> = persistentListOf(),
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

    /** Порядок объявления — порядок фильтров в UI (как списки в библиотеке, «Любимое» в конце). */
    enum class ListFilter(val list: UserAnimeList?) {
        WATCHING(UserAnimeList.WATCHING),
        PLANNED(UserAnimeList.PLANNED),
        COMPLETED(UserAnimeList.COMPLETED),
        POSTPONED(UserAnimeList.POSTPONED),
        DROPPED(UserAnimeList.DROPPED),
        FAVORITES(null),
    }

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetryOverviewSelected : Event
        data class TabSelected(val tab: Tab) : Event
        data class ListFilterSelected(val filter: ListFilter) : Event
        data object RetryTabSelected : Event
        data class AnimeSelected(val animeId: Int) : Event
        data class CollectionSelected(val collectionId: Int) : Event
        data class PostSelected(val postId: Int) : Event
        data class ReviewSelected(val reviewId: Int) : Event
        data class FriendSelected(val userId: Int) : Event
        data object FriendshipActionSelected : Event
        data object LoginToFriendSelected : Event
        data object MessageSelected : Event
        data object CommentsSelected : Event
        data object RetryCommentsSelected : Event
        data class CommentAnimeSelected(val animeId: Int) : Event
    }

    sealed interface Effect : UiEffect
}
