package su.afk.yummy.tv.feature.account.userprofile

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.comments.CommentTargetType
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.FriendshipStatus
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.collection.repository.CollectionMutationRepository
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState.Event
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState.ListFilter
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState.Tab
import su.afk.yummy.tv.feature.account.userprofile.handler.FriendshipFetchResult
import su.afk.yummy.tv.feature.account.userprofile.handler.FriendshipOwnership
import su.afk.yummy.tv.feature.account.userprofile.handler.UserProfileContentHandler
import su.afk.yummy.tv.feature.account.userprofile.handler.UserProfileFriendshipHandler
import su.afk.yummy.tv.feature.account.userprofile.handler.UserProfilePagingFetchHandler
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.comments.ICommentsNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.messages.IMessagesNavigator
import su.afk.yummy.tv.feature.posts.IPostsNavigator
import su.afk.yummy.tv.feature.reviews.IReviewsNavigator

class UserProfileViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val accountNavigator: IAccountNavigator = mockk()
    private val collectionNavigator: ICollectionNavigator = mockk()
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val messagesNavigator: IMessagesNavigator = mockk()
    private val postsNavigator: IPostsNavigator = mockk()
    private val reviewsNavigator: IReviewsNavigator = mockk()
    private val commentsNavigator: ICommentsNavigator = mockk()
    private val contentHandler: UserProfileContentHandler = mockk()
    private val pagingFetchHandler: UserProfilePagingFetchHandler = mockk(relaxed = true)
    private val friendshipHandler: UserProfileFriendshipHandler = mockk()
    private val collectionMutationNotifier: CollectionMutationRepository = mockk()
    private val analytics: UserProfileAnalytics = mockk(relaxed = true)

    private val navKey: NavKey = mockk()
    private val stats: UserStats = mockk()
    private val listItem: UserAnimeListItem = mockk()

    @Before
    fun setUp() {
        every { collectionMutationNotifier.version } returns MutableStateFlow(0L)
        coEvery { contentHandler.loadOverview(USER_ID) } returns
            Result.success(UserProfileSummary(userId = USER_ID, nickname = "sandy") to stats)
        coEvery { contentHandler.loadLists(any(), any(), any()) } returns Result.success(listOf(listItem))
        coEvery { friendshipHandler.resolveOwnership(USER_ID) } returns
            FriendshipOwnership(isAuthorized = true, isOwnProfile = false, sessionUserId = SESSION_ID)
        coEvery { friendshipHandler.fetchStatus(SESSION_ID, USER_ID) } returns
            FriendshipFetchResult.Success(FriendshipStatus.NONE)
        every { detailsNavigator.getDetailsDest(any()) } returns navKey
        every { collectionNavigator.getCollectionDest(any()) } returns navKey
        every { postsNavigator.details(any()) } returns navKey
        every { reviewsNavigator.details(any()) } returns navKey
        every { accountNavigator.getUserProfileDest(any()) } returns navKey
        every { accountNavigator.getAccountDest() } returns navKey
        every { messagesNavigator.chat(any(), any(), any()) } returns navKey
        every { commentsNavigator.getCommentsDest(any(), any()) } returns navKey
    }

    private fun createViewModel(userId: Int = USER_ID) = UserProfileViewModel(
        userId = userId,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        accountNavigator = accountNavigator,
        collectionNavigator = collectionNavigator,
        detailsNavigator = detailsNavigator,
        messagesNavigator = messagesNavigator,
        postsNavigator = postsNavigator,
        reviewsNavigator = reviewsNavigator,
        commentsNavigator = commentsNavigator,
        contentHandler = contentHandler,
        pagingFetchHandler = pagingFetchHandler,
        friendshipHandler = friendshipHandler,
        collectionMutationNotifier = collectionMutationNotifier,
        analytics = analytics,
    )

    @Test
    fun `opening loads the overview and tracks the screen`() {
        val state = createViewModel().currentState

        assertFalse(state.isOverviewLoading)
        assertFalse(state.overviewError)
        assertEquals("sandy", state.profile?.nickname)
        assertEquals(stats, state.stats)
        verify(exactly = 1) { analytics.eventScreenOpened(USER_ID) }
    }

    @Test
    fun `failed overview shows the error and retry reloads it`() {
        coEvery { contentHandler.loadOverview(USER_ID) } returns Result.failure(IllegalStateException("boom"))
        val vm = createViewModel()
        assertTrue(vm.currentState.overviewError)
        assertFalse(vm.currentState.isOverviewLoading)
        coEvery { contentHandler.loadOverview(USER_ID) } returns
            Result.success(UserProfileSummary(userId = USER_ID, nickname = "again") to stats)

        vm.setEvent(Event.RetryOverviewSelected)

        assertFalse(vm.currentState.overviewError)
        assertEquals("again", vm.currentState.profile?.nickname)
    }

    @Test
    fun `invalid user id never loads anything`() {
        val vm = createViewModel(userId = 0)

        assertTrue(vm.currentState.isOverviewLoading)
        coVerify(exactly = 0) { contentHandler.loadOverview(any()) }
    }

    @Test
    fun `viewer of another profile gets the friendship status`() {
        coEvery { friendshipHandler.fetchStatus(SESSION_ID, USER_ID) } returns
            FriendshipFetchResult.Success(FriendshipStatus.FRIENDS)

        val state = createViewModel().currentState

        assertTrue(state.isAuthorized)
        assertFalse(state.isOwnProfile)
        assertEquals(FriendshipStatus.FRIENDS, state.friendshipStatus)
        assertFalse(state.isFriendshipLoading)
    }

    @Test
    fun `own profile does not load the friendship`() {
        coEvery { friendshipHandler.resolveOwnership(USER_ID) } returns
            FriendshipOwnership(isAuthorized = true, isOwnProfile = true, sessionUserId = USER_ID)

        val state = createViewModel().currentState

        assertTrue(state.isOwnProfile)
        assertFalse(state.isFriendshipLoading)
        coVerify(exactly = 0) { friendshipHandler.fetchStatus(any(), any()) }
    }

    @Test
    fun `guest does not load the friendship`() {
        coEvery { friendshipHandler.resolveOwnership(USER_ID) } returns
            FriendshipOwnership(isAuthorized = false, isOwnProfile = false, sessionUserId = 0)

        val state = createViewModel().currentState

        assertFalse(state.isAuthorized)
        coVerify(exactly = 0) { friendshipHandler.fetchStatus(any(), any()) }
    }

    @Test
    fun `friendship action updates the status`() {
        coEvery { friendshipHandler.updateFriendship(SESSION_ID, USER_ID, FriendshipStatus.NONE) } returns
            FriendshipFetchResult.Success(FriendshipStatus.SENT_REQUESTS)
        val vm = createViewModel()

        vm.setEvent(Event.FriendshipActionSelected)

        assertEquals(FriendshipStatus.SENT_REQUESTS, vm.currentState.friendshipStatus)
        assertFalse(vm.currentState.friendshipError)
    }

    @Test
    fun `failed friendship action sets the error flag`() {
        coEvery { friendshipHandler.updateFriendship(any(), any(), any()) } returns FriendshipFetchResult.Failure
        val vm = createViewModel()

        vm.setEvent(Event.FriendshipActionSelected)

        assertTrue(vm.currentState.friendshipError)
        assertFalse(vm.currentState.isFriendshipLoading)
    }

    @Test
    fun `lists tab loads the lists once`() {
        val vm = createViewModel()

        vm.setEvent(Event.TabSelected(Tab.LISTS))
        vm.setEvent(Event.TabSelected(Tab.OVERVIEW))
        vm.setEvent(Event.TabSelected(Tab.LISTS))

        assertEquals(Tab.LISTS, vm.currentState.selectedTab)
        assertEquals(listOf(listItem), vm.currentState.lists.items)
        assertTrue(vm.currentState.lists.loaded)
        coVerify(exactly = 1) { contentHandler.loadLists(USER_ID, ListFilter.WATCHING, false) }
    }

    @Test
    fun `list filter reloads the lists forcibly`() {
        val vm = createViewModel()
        vm.setEvent(Event.TabSelected(Tab.LISTS))

        vm.setEvent(Event.ListFilterSelected(ListFilter.FAVORITES))

        assertEquals(ListFilter.FAVORITES, vm.currentState.selectedList)
        coVerify(exactly = 1) { contentHandler.loadLists(USER_ID, ListFilter.FAVORITES, true) }
    }

    @Test
    fun `failed lists are marked with an error and retry loads them again`() {
        coEvery { contentHandler.loadLists(any(), any(), any()) } returns Result.failure(IllegalStateException("boom"))
        val vm = createViewModel()
        vm.setEvent(Event.TabSelected(Tab.LISTS))
        assertTrue(vm.currentState.lists.error)
        coEvery { contentHandler.loadLists(any(), any(), any()) } returns Result.success(listOf(listItem))

        vm.setEvent(Event.RetryTabSelected)

        assertFalse(vm.currentState.lists.error)
        assertEquals(listOf(listItem), vm.currentState.lists.items)
    }

    @Test
    fun `anime selection opens the title only for a valid id`() {
        val vm = createViewModel()

        vm.setEvent(Event.AnimeSelected(0))
        vm.setEvent(Event.AnimeSelected(11))

        verify(exactly = 1) { nav.navigate(navKey) }
        verify(exactly = 1) { detailsNavigator.getDetailsDest(11) }
    }

    @Test
    fun `collection post and review selections navigate`() {
        val vm = createViewModel()

        vm.setEvent(Event.CollectionSelected(1))
        vm.setEvent(Event.PostSelected(2))
        vm.setEvent(Event.ReviewSelected(3))
        vm.setEvent(Event.PostSelected(0))

        verify(exactly = 3) { nav.navigate(navKey) }
        verify(exactly = 1) { collectionNavigator.getCollectionDest(1) }
        verify(exactly = 1) { postsNavigator.details(2) }
        verify(exactly = 1) { reviewsNavigator.details(3) }
    }

    @Test
    fun `friend selection opens that profile`() {
        createViewModel().setEvent(Event.FriendSelected(77))

        verify(exactly = 1) { accountNavigator.getUserProfileDest(77) }
        verify(exactly = 1) { nav.navigate(navKey) }
    }

    @Test
    fun `message from an authorized viewer opens the chat with the profile data`() {
        val vm = createViewModel()

        vm.setEvent(Event.MessageSelected)

        verify(exactly = 1) { messagesNavigator.chat(USER_ID, "sandy", null) }
        verify(exactly = 1) { nav.navigate(navKey) }
    }

    @Test
    fun `message from a guest opens the sign-in`() {
        coEvery { friendshipHandler.resolveOwnership(USER_ID) } returns
            FriendshipOwnership(isAuthorized = false, isOwnProfile = false, sessionUserId = 0)
        val vm = createViewModel()

        vm.setEvent(Event.MessageSelected)

        verify(exactly = 1) { accountNavigator.getAccountDest() }
        verify(exactly = 0) { messagesNavigator.chat(any(), any(), any()) }
    }

    @Test
    fun `comments open the user comments`() {
        createViewModel().setEvent(Event.CommentsSelected)

        verify(exactly = 1) { commentsNavigator.getCommentsDest(CommentTargetType.USER, USER_ID) }
    }

    @Test
    fun `login to friend opens the account screen`() {
        createViewModel().setEvent(Event.LoginToFriendSelected)

        verify(exactly = 1) { nav.navigate(navKey) }
    }

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val USER_ID = 10
        const val SESSION_ID = 99
    }
}
