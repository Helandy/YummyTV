package su.afk.yummy.tv.feature.account.userprofile

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.utils.paging.OffsetPage
import su.afk.yummy.tv.core.utils.paging.OffsetPagingSource
import su.afk.yummy.tv.domain.collection.CollectionMutationNotifier
import su.afk.yummy.tv.domain.comments.model.CommentTargetType
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.account.userprofile.handler.FriendshipFetchResult
import su.afk.yummy.tv.feature.account.userprofile.handler.UserProfileContentHandler
import su.afk.yummy.tv.feature.account.userprofile.handler.UserProfileFriendshipHandler
import su.afk.yummy.tv.feature.account.userprofile.handler.UserProfilePagingFetchHandler
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.comments.ICommentsNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.messages.IMessagesNavigator
import su.afk.yummy.tv.feature.posts.IPostsNavigator
import su.afk.yummy.tv.feature.reviews.IReviewsNavigator

private const val USER_PROFILE_PAGE_SIZE = 20

@HiltViewModel(assistedFactory = UserProfileViewModel.Factory::class)
class UserProfileViewModel @AssistedInject internal constructor(
    @Assisted private val userId: Int,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val accountNavigator: IAccountNavigator,
    private val collectionNavigator: ICollectionNavigator,
    private val detailsNavigator: IDetailsNavigator,
    private val messagesNavigator: IMessagesNavigator,
    private val postsNavigator: IPostsNavigator,
    private val reviewsNavigator: IReviewsNavigator,
    private val commentsNavigator: ICommentsNavigator,
    private val contentHandler: UserProfileContentHandler,
    private val pagingFetchHandler: UserProfilePagingFetchHandler,
    private val friendshipHandler: UserProfileFriendshipHandler,
    private val collectionMutationNotifier: CollectionMutationNotifier,
    private val analytics: UserProfileAnalytics,
) : BaseViewModel<UserProfileState.State, UserProfileState.Event, UserProfileState.Effect>() {

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
        loadFriendship()
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

            is UserProfileState.Event.CollectionSelected -> {
                if (event.collectionId > 0) {
                    nav.navigate(collectionNavigator.getCollectionDest(event.collectionId))
                }
            }

            is UserProfileState.Event.PostSelected -> {
                if (event.postId > 0) {
                    nav.navigate(postsNavigator.details(event.postId))
                }
            }

            is UserProfileState.Event.ReviewSelected -> {
                if (event.reviewId > 0) {
                    nav.navigate(reviewsNavigator.details(event.reviewId))
                }
            }

            is UserProfileState.Event.FriendSelected -> {
                if (event.userId > 0) {
                    analytics.eventFriendSelected(userId, event.userId)
                    nav.navigate(accountNavigator.getUserProfileDest(event.userId))
                }
            }

            UserProfileState.Event.FriendshipActionSelected -> updateFriendship()
            UserProfileState.Event.LoginToFriendSelected ->
                nav.navigate(accountNavigator.getAccountDest())

            UserProfileState.Event.MessageSelected -> {
                if (currentState.isAuthorized && !currentState.isOwnProfile && userId > 0) {
                    nav.navigate(
                        messagesNavigator.chat(
                            userId = userId,
                            nickname = currentState.profile?.nickname.orEmpty(),
                            avatarUrl = currentState.profile?.avatarUrl,
                        )
                    )
                } else if (!currentState.isAuthorized) {
                    nav.navigate(accountNavigator.getAccountDest())
                }
            }

            UserProfileState.Event.CommentsSelected -> if (userId > 0) {
                nav.navigate(commentsNavigator.getCommentsDest(CommentTargetType.USER, userId))
            }
        }
    }

    private fun loadFriendship() = viewModelScope.launch {
        val ownership = friendshipHandler.resolveOwnership(userId)
        setState {
            copy(
                isAuthorized = ownership.isAuthorized,
                isOwnProfile = ownership.isOwnProfile,
                isFriendshipLoading = ownership.isAuthorized && !ownership.isOwnProfile,
                friendshipError = false,
            )
        }
        if (!ownership.isAuthorized || ownership.isOwnProfile ||
            ownership.sessionUserId <= 0 || userId <= 0
        ) {
            return@launch
        }
        when (val result = friendshipHandler.fetchStatus(ownership.sessionUserId, userId)) {
            is FriendshipFetchResult.Success ->
                setState { copy(friendshipStatus = result.status, isFriendshipLoading = false) }

            FriendshipFetchResult.Failure -> setState { copy(isFriendshipLoading = false) }
        }
    }

    private fun updateFriendship() {
        val state = currentState
        if (!state.isAuthorized || state.isOwnProfile || state.isFriendshipLoading) return
        viewModelScope.launch {
            val ownership = friendshipHandler.resolveOwnership(userId)
            if (!ownership.isAuthorized || ownership.sessionUserId <= 0) return@launch
            setState { copy(isFriendshipLoading = true, friendshipError = false) }
            val result = friendshipHandler.updateFriendship(
                sessionUserId = ownership.sessionUserId,
                userId = userId,
                currentStatus = state.friendshipStatus,
            )
            when (result) {
                is FriendshipFetchResult.Success ->
                    setState { copy(friendshipStatus = result.status, isFriendshipLoading = false) }

                FriendshipFetchResult.Failure ->
                    setState { copy(isFriendshipLoading = false, friendshipError = true) }
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
            contentHandler.loadOverview(userId).fold(
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
            contentHandler.loadLists(userId, currentState.selectedList, force).fold(
                onSuccess = { items -> setState { copy(lists = lists.finish(items)) } },
                onFailure = { error ->
                    analytics.eventTabLoadError(userId, UserProfileState.Tab.LISTS, error)
                    setState { copy(lists = lists.fail()) }
                },
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createCollectionsFlow() =
        collectionMutationNotifier.version.flatMapLatest {
            createPagingFlow(
                tab = UserProfileState.Tab.COLLECTIONS,
                fetch = { limit, offset ->
                    pagingFetchHandler.fetchCollections(
                        userId,
                        limit,
                        offset
                    )
                },
            )
        }.cachedIn(viewModelScope)

    private fun createPostsFlow() =
        createPagingFlow(
            tab = UserProfileState.Tab.POSTS,
            fetch = { limit, offset -> pagingFetchHandler.fetchPosts(userId, limit, offset) },
        ).cachedIn(viewModelScope)

    private fun createReviewsFlow() =
        createPagingFlow(
            tab = UserProfileState.Tab.REVIEWS,
            fetch = { limit, offset -> pagingFetchHandler.fetchReviews(userId, limit, offset) },
        ).cachedIn(viewModelScope)

    private fun createFriendsFlow() =
        createPagingFlow(
            tab = UserProfileState.Tab.FRIENDS,
            fetch = { limit, offset -> pagingFetchHandler.fetchFriends(userId, limit, offset) },
        ).cachedIn(viewModelScope)

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
                runSuspendCatching { fetch(limit, offset) }.fold(
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
    ).flow

    private fun <T> UserProfileState.PagedContent<T>.startLoading() =
        copy(
            isLoading = true,
            error = false,
        )

    private fun <T> UserProfileState.PagedContent<T>.finish(
        incoming: List<T>,
    ) = copy(
        items = incoming.toImmutableList(),
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
