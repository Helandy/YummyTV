package su.afk.yummy.tv.feature.account.userprofile

import androidx.lifecycle.SavedStateHandle
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
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
import su.afk.yummy.tv.core.utils.OffsetPage
import su.afk.yummy.tv.core.utils.OffsetPagingSource
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
    private val analytics: UserProfileAnalytics,
) : BaseViewModelNew<UserProfileState.State, UserProfileState.Event, UserProfileState.Effect>(
    savedStateHandle
) {

    @AssistedFactory
    interface Factory {
        fun create(userId: Int): UserProfileViewModel
    }

    override fun createInitialState() = UserProfileState.State(
        userId = userId,
        collections = createCollectionsFlow(),
        posts = createPostsFlow(),
        reviews = createReviewsFlow(),
        friends = createFriendsFlow(),
    )

    init {
        analytics.eventScreenOpened(userId)
        loadOverview()
    }

    override fun onEvent(event: UserProfileState.Event) {
        when (event) {
            UserProfileState.Event.BackSelected -> nav.back()
            UserProfileState.Event.RetryOverviewSelected -> {
                analytics.eventRetryOverviewSelected(userId)
                loadOverview()
            }

            is UserProfileState.Event.TabSelected -> {
                if (event.tab != currentState.selectedTab) {
                    analytics.eventTabSelected(userId, event.tab)
                }
                setState { copy(selectedTab = event.tab) }
                loadTabIfNeeded(event.tab)
            }

            is UserProfileState.Event.ListFilterSelected -> {
                if (event.filter != currentState.selectedList) {
                    analytics.eventListFilterSelected(userId, event.filter)
                    setState {
                        copy(
                            selectedList = event.filter,
                            lists = UserProfileState.PagedContent(),
                        )
                    }
                }
                loadLists(force = true)
            }

            UserProfileState.Event.RetryTabSelected -> {
                analytics.eventRetryTabSelected(userId, currentState.selectedTab)
                loadSelectedTab(force = true)
            }

            is UserProfileState.Event.AnimeSelected -> {
                if (event.animeId > 0) {
                    analytics.eventAnimeSelected(userId, event.animeId)
                    nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
                }
            }

            is UserProfileState.Event.FriendSelected -> {
                if (event.userId > 0) {
                    analytics.eventFriendSelected(userId, event.userId)
                    nav.navigate(accountNavigator.getUserProfileDest(event.userId))
                }
            }
        }
    }

    override fun onRetry() {
        if (currentState.overviewError) {
            analytics.eventRetryOverviewSelected(userId)
            loadOverview()
        } else {
            analytics.eventRetryTabSelected(userId, currentState.selectedTab)
            loadSelectedTab(force = true)
        }
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
                onFailure = { error ->
                    analytics.eventOverviewLoadError(userId, error)
                    setState { copy(isOverviewLoading = false, overviewError = true) }
                },
            )
        }
    }

    private fun loadTabIfNeeded(tab: UserProfileState.Tab) {
        when (tab) {
            UserProfileState.Tab.OVERVIEW -> Unit
            UserProfileState.Tab.LISTS -> if (!currentState.lists.loaded) loadLists()
            UserProfileState.Tab.COLLECTIONS,
            UserProfileState.Tab.POSTS,
            UserProfileState.Tab.REVIEWS,
            UserProfileState.Tab.FRIENDS -> Unit
        }
    }

    private fun loadSelectedTab(force: Boolean = false) {
        when (currentState.selectedTab) {
            UserProfileState.Tab.OVERVIEW -> loadOverview()
            UserProfileState.Tab.LISTS -> loadLists(force = force)
            UserProfileState.Tab.COLLECTIONS,
            UserProfileState.Tab.POSTS,
            UserProfileState.Tab.REVIEWS,
            UserProfileState.Tab.FRIENDS -> Unit
        }
    }

    private fun loadLists(force: Boolean = false) {
        val content = currentState.lists
        if (content.isLoading || (!force && content.loaded)) return
        viewModelScope.launch {
            setState { copy(lists = content.startLoading()) }
            runCatching {
                val filter = currentState.selectedList
                if (filter == UserProfileState.ListFilter.FAVORITES) {
                    getUserFavoriteAnimeList(userId, forceRefresh = force)
                } else {
                    getUserAnimeList(userId, requireNotNull(filter.list), forceRefresh = force)
                }
            }.fold(
                onSuccess = { items -> setState { copy(lists = lists.finish(items)) } },
                onFailure = { error ->
                    analytics.eventTabLoadError(userId, UserProfileState.Tab.LISTS, error)
                    setState { copy(lists = lists.fail()) }
                },
            )
        }
    }

    private fun createCollectionsFlow() =
        createPagingFlow(
            tab = UserProfileState.Tab.COLLECTIONS,
            fetch = { limit, offset -> getUserCollections(userId, limit, offset) },
        )

    private fun createPostsFlow() =
        createPagingFlow(
            tab = UserProfileState.Tab.POSTS,
            fetch = { limit, offset -> getUserPosts(userId, limit, offset) },
        )

    private fun createReviewsFlow() =
        createPagingFlow(
            tab = UserProfileState.Tab.REVIEWS,
            fetch = { limit, offset -> getUserReviews(userId, limit, offset) },
        )

    private fun createFriendsFlow() =
        createPagingFlow(
            tab = UserProfileState.Tab.FRIENDS,
            fetch = { limit, offset -> getUserFriends(userId, limit, offset) },
        )

    private fun <T : Any> createPagingFlow(
        tab: UserProfileState.Tab,
        fetch: suspend (limit: Int, offset: Int) -> List<T>,
    ) = Pager(
        config = PagingConfig(
            pageSize = USER_PROFILE_PAGE_SIZE,
            initialLoadSize = USER_PROFILE_PAGE_SIZE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = {
            OffsetPagingSource { limit, offset ->
                runCatching { fetch(limit, offset) }.fold(
                    onSuccess = { items ->
                        OffsetPage(
                            items = items,
                            nextOffset = offset + items.size,
                            canLoadMore = items.size >= limit,
                        )
                    },
                    onFailure = { error ->
                        analytics.eventTabLoadError(userId, tab, error)
                        throw error
                    },
                )
            }
        },
    ).flow.cachedIn(viewModelScope)

    private fun <T> UserProfileState.PagedContent<T>.startLoading() =
        copy(
            isLoading = true,
            error = false,
        )

    private fun <T> UserProfileState.PagedContent<T>.finish(
        incoming: List<T>,
    ) = copy(
        items = incoming,
        isLoading = false,
        error = false,
        loaded = true,
    )

    private fun <T> UserProfileState.PagedContent<T>.fail() =
        copy(
            isLoading = false,
            error = true,
            loaded = true,
        )
}
