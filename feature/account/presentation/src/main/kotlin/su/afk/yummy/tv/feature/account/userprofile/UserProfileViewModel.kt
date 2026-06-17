package su.afk.yummy.tv.feature.account.userprofile

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.account.usecase.GetUserAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserCollectionsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserFavoriteAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserFriendsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserPostsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserProfileSummaryUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserReviewsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserStatsUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator

private const val USER_PROFILE_PAGE_SIZE = 20

@HiltViewModel(assistedFactory = UserProfileViewModel.Factory::class)
class UserProfileViewModel @AssistedInject internal constructor(
    @Assisted private val userId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val accountNavigator: IAccountNavigator,
    private val detailsNavigator: IDetailsNavigator,
    private val getUserProfileSummary: GetUserProfileSummaryUseCase,
    private val getUserStats: GetUserStatsUseCase,
    private val getUserAnimeList: GetUserAnimeListUseCase,
    private val getUserFavoriteAnimeList: GetUserFavoriteAnimeListUseCase,
    private val getUserCollections: GetUserCollectionsUseCase,
    private val getUserPosts: GetUserPostsUseCase,
    private val getUserReviews: GetUserReviewsUseCase,
    private val getUserFriends: GetUserFriendsUseCase,
) : BaseViewModelNew<UserProfileState.State, UserProfileState.Event, UserProfileState.Effect>(
    savedStateHandle
) {

    @AssistedFactory
    interface Factory {
        fun create(userId: Int): UserProfileViewModel
    }

    override fun createInitialState() = UserProfileState.State(userId = userId)

    init {
        loadOverview()
    }

    override fun onEvent(event: UserProfileState.Event) {
        when (event) {
            UserProfileState.Event.BackSelected -> nav.back()
            UserProfileState.Event.RetryOverviewSelected -> loadOverview()
            is UserProfileState.Event.TabSelected -> {
                setState { copy(selectedTab = event.tab) }
                loadTabIfNeeded(event.tab)
            }

            is UserProfileState.Event.ListFilterSelected -> {
                if (event.filter != currentState.selectedList) {
                    setState {
                        copy(
                            selectedList = event.filter,
                            lists = UserProfileState.PagedContent(),
                        )
                    }
                }
                loadLists(force = true)
            }

            UserProfileState.Event.LoadMoreSelected -> loadSelectedTab(append = true)
            UserProfileState.Event.RetryTabSelected -> loadSelectedTab(force = true)
            is UserProfileState.Event.AnimeSelected -> {
                if (event.animeId > 0) nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }

            is UserProfileState.Event.FriendSelected -> {
                if (event.userId > 0) nav.navigate(accountNavigator.getUserProfileDest(event.userId))
            }
        }
    }

    override fun onRetry() {
        if (currentState.overviewError) loadOverview() else loadSelectedTab(force = true)
    }

    private fun loadOverview() {
        if (userId <= 0) return
        viewModelScope.launch {
            setState { copy(isOverviewLoading = true, overviewError = false) }
            runCatching {
                coroutineScope {
                    val profile = async { getUserProfileSummary(userId) }
                    val stats = async { getUserStats(userId) }
                    profile.await() to stats.await()
                }
            }.fold(
                onSuccess = { (profile, stats) ->
                    setState {
                        copy(
                            profile = profile,
                            stats = stats,
                            isOverviewLoading = false,
                            overviewError = false,
                        )
                    }
                },
                onFailure = {
                    setState { copy(isOverviewLoading = false, overviewError = true) }
                },
            )
        }
    }

    private fun loadTabIfNeeded(tab: UserProfileState.Tab) {
        when (tab) {
            UserProfileState.Tab.OVERVIEW -> Unit
            UserProfileState.Tab.LISTS -> if (!currentState.lists.loaded) loadLists()
            UserProfileState.Tab.COLLECTIONS -> if (!currentState.collections.loaded) loadCollections()
            UserProfileState.Tab.POSTS -> if (!currentState.posts.loaded) loadPosts()
            UserProfileState.Tab.REVIEWS -> if (!currentState.reviews.loaded) loadReviews()
            UserProfileState.Tab.FRIENDS -> if (!currentState.friends.loaded) loadFriends()
        }
    }

    private fun loadSelectedTab(force: Boolean = false, append: Boolean = false) {
        when (currentState.selectedTab) {
            UserProfileState.Tab.OVERVIEW -> loadOverview()
            UserProfileState.Tab.LISTS -> loadLists(force = force, append = append)
            UserProfileState.Tab.COLLECTIONS -> loadCollections(force = force, append = append)
            UserProfileState.Tab.POSTS -> loadPosts(force = force, append = append)
            UserProfileState.Tab.REVIEWS -> loadReviews(force = force, append = append)
            UserProfileState.Tab.FRIENDS -> loadFriends(force = force, append = append)
        }
    }

    private fun loadLists(force: Boolean = false, append: Boolean = false) {
        val content = currentState.lists
        if (shouldSkipLoad(content, force, append)) return
        viewModelScope.launch {
            setState { copy(lists = content.startLoading(append)) }
            runCatching {
                val filter = currentState.selectedList
                val items = if (filter == UserProfileState.ListFilter.FAVORITES) {
                    getUserFavoriteAnimeList(userId, forceRefresh = force)
                } else {
                    getUserAnimeList(userId, requireNotNull(filter.list), forceRefresh = force)
                }
                if (append) emptyList() else items
            }.fold(
                onSuccess = { items -> setState { copy(lists = lists.finish(items, append)) } },
                onFailure = { setState { copy(lists = lists.fail(append)) } },
            )
        }
    }

    private fun loadCollections(force: Boolean = false, append: Boolean = false) =
        loadPaged(
            content = currentState.collections,
            force = force,
            append = append,
            fetch = { offset -> getUserCollections(userId, USER_PROFILE_PAGE_SIZE, offset) },
            update = { copy(collections = it) },
        )

    private fun loadPosts(force: Boolean = false, append: Boolean = false) =
        loadPaged(
            content = currentState.posts,
            force = force,
            append = append,
            fetch = { offset -> getUserPosts(userId, USER_PROFILE_PAGE_SIZE, offset) },
            update = { copy(posts = it) },
        )

    private fun loadReviews(force: Boolean = false, append: Boolean = false) =
        loadPaged(
            content = currentState.reviews,
            force = force,
            append = append,
            fetch = { offset -> getUserReviews(userId, USER_PROFILE_PAGE_SIZE, offset) },
            update = { copy(reviews = it) },
        )

    private fun loadFriends(force: Boolean = false, append: Boolean = false) =
        loadPaged(
            content = currentState.friends,
            force = force,
            append = append,
            fetch = { offset -> getUserFriends(userId, USER_PROFILE_PAGE_SIZE, offset) },
            update = { copy(friends = it) },
        )

    private fun <T> loadPaged(
        content: UserProfileState.PagedContent<T>,
        force: Boolean,
        append: Boolean,
        fetch: suspend (offset: Int) -> List<T>,
        update: UserProfileState.State.(UserProfileState.PagedContent<T>) -> UserProfileState.State,
    ) {
        if (shouldSkipLoad(content, force, append)) return
        viewModelScope.launch {
            val offset = if (append) content.items.size else 0
            setState { update(content.startLoading(append)) }
            runCatching { fetch(offset) }.fold(
                onSuccess = { items ->
                    setState { update(content.finish(items, append)) }
                },
                onFailure = {
                    setState { update(content.fail(append)) }
                },
            )
        }
    }

    private fun <T> shouldSkipLoad(
        content: UserProfileState.PagedContent<T>,
        force: Boolean,
        append: Boolean,
    ): Boolean {
        if (content.isLoading || content.isLoadingMore) return true
        if (append && !content.hasMore) return true
        if (!force && !append && content.loaded) return true
        return false
    }

    private fun <T> UserProfileState.PagedContent<T>.startLoading(append: Boolean) =
        copy(
            isLoading = !append,
            isLoadingMore = append,
            error = false,
        )

    private fun <T> UserProfileState.PagedContent<T>.finish(
        incoming: List<T>,
        append: Boolean,
    ) = copy(
        items = if (append) items + incoming else incoming,
        isLoading = false,
        isLoadingMore = false,
        hasMore = incoming.size >= USER_PROFILE_PAGE_SIZE,
        error = false,
        loaded = true,
    )

    private fun <T> UserProfileState.PagedContent<T>.fail(append: Boolean) =
        copy(
            isLoading = false,
            isLoadingMore = false,
            error = true,
            loaded = !append,
        )
}
