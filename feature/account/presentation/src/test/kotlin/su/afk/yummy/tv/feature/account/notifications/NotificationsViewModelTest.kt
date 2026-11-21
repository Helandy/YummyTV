package su.afk.yummy.tv.feature.account.notifications

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.comments.CommentTargetType
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.account.account.AccountAnalytics
import su.afk.yummy.tv.feature.account.account.handler.AccountHubHandler
import su.afk.yummy.tv.feature.account.account.handler.AccountNotificationHandler
import su.afk.yummy.tv.feature.account.account.handler.AccountNotificationMutationHandler
import su.afk.yummy.tv.feature.account.account.handler.AccountNotificationMutationOutcome
import su.afk.yummy.tv.feature.account.account.handler.AccountOpenNotificationResult
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.comments.ICommentsNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator

class NotificationsViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val settingsStore: YaniAccountSettingsStore = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk()
    private val detailsNavigator: IDetailsNavigator = mockk(relaxed = true)
    private val commentsNavigator: ICommentsNavigator = mockk(relaxed = true)
    private val accountNavigator: IAccountNavigator = mockk(relaxed = true)
    private val hubHandler: AccountHubHandler = mockk()
    private val notificationHandler: AccountNotificationHandler = mockk()
    private val notificationMutationHandler: AccountNotificationMutationHandler = mockk()
    private val analytics: AccountAnalytics = mockk(relaxed = true)

    private val session = MutableStateFlow(AccountSession(isAuthorized = true, userId = USER_ID))

    @Before
    fun setUp() {
        every { accountRepository.observeSession() } returns session
        stubNotifications()
    }

    private fun createViewModel() = NotificationsViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        settingsStore = settingsStore,
        observeAccountSession = ObserveAccountSessionUseCase(accountRepository),
        detailsNavigator = detailsNavigator,
        commentsNavigator = commentsNavigator,
        accountNavigator = accountNavigator,
        hubHandler = hubHandler,
        notificationHandler = notificationHandler,
        notificationMutationHandler = notificationMutationHandler,
        analytics = analytics,
    )

    private fun notification(id: Int, viewed: Boolean = false, slug: String? = null) = ProfileNotification(
        id = id,
        dateSeconds = 0L,
        title = "t$id",
        text = "",
        clickUri = "",
        type = "anime",
        subType = "",
        viewed = viewed,
        objectId = null,
        animeSlug = slug,
        isNewEpisode = slug != null,
    )

    private fun stubNotifications(
        notifications: Result<List<ProfileNotification>> = Result.success(emptyList()),
        counts: Result<List<NotificationCount>> = Result.success(emptyList()),
        cached: List<ProfileNotification>? = null,
    ) {
        coEvery { hubHandler.loadCachedNotifications() } returns cached
        coEvery { hubHandler.loadNotifications() } returns notifications
        coEvery { hubHandler.loadNotificationCounts() } returns counts
    }

    @Test
    fun `notifications are loaded when the session is signed in`() {
        stubNotifications(
            notifications = Result.success(listOf(notification(1), notification(2, viewed = true))),
            counts = Result.success(listOf(NotificationCount("anime", 1))),
        )

        val state = createViewModel().currentState

        assertEquals(listOf(1, 2), state.notifications.map { it.id })
        assertEquals(1, state.unreadNotificationCount)
        assertFalse(state.isNotificationsLoading)
        assertNull(state.error)
    }

    @Test
    fun `cached notifications are shown before the network answers`() {
        stubNotifications(
            notifications = Result.failure(IllegalStateException("offline")),
            cached = listOf(notification(7)),
        )

        val state = createViewModel().currentState

        assertEquals(listOf(7), state.notifications.map { it.id })
        assertEquals(AccountUiError.LOAD_NOTIFICATIONS_FAILED, state.error)
    }

    @Test
    fun `signing out clears the list`() {
        stubNotifications(notifications = Result.success(listOf(notification(1))))
        val vm = createViewModel()
        assertEquals(1, vm.currentState.notifications.size)

        session.value = AccountSession(isAuthorized = false, userId = 0)

        assertTrue(vm.currentState.notifications.isEmpty())
    }

    @Test
    fun `failed load shows the error and refresh recovers`() {
        stubNotifications(notifications = Result.failure(IllegalStateException("boom")))
        val vm = createViewModel()
        assertEquals(AccountUiError.LOAD_NOTIFICATIONS_FAILED, vm.currentState.error)

        stubNotifications(notifications = Result.success(listOf(notification(1))))
        vm.setEvent(NotificationsState.Event.RefreshSelected)

        assertNull(vm.currentState.error)
        assertEquals(listOf(1), vm.currentState.notifications.map { it.id })
    }

    @Test
    fun `my subscriptions and back are navigation events`() {
        val vm = createViewModel()

        vm.setEvent(NotificationsState.Event.MySubscriptionsSelected)
        vm.setEvent(NotificationsState.Event.BackSelected)

        verify(exactly = 1) { accountNavigator.getMySubscriptionsDest() }
        verify(exactly = 1) { nav.navigate(any()) }
        verify(exactly = 1) { nav.back() }
    }

    @Test
    fun `a full first page offers more notifications and a short one does not`() {
        stubNotifications(notifications = Result.success(List(20) { notification(it) }))
        assertTrue(createViewModel().currentState.hasMoreNotifications)

        stubNotifications(notifications = Result.success(List(19) { notification(it) }))
        assertFalse(createViewModel().currentState.hasMoreNotifications)
    }

    @Test
    fun `load more appends the next page by offset without duplicates`() {
        stubNotifications(notifications = Result.success(List(20) { notification(it) }))
        coEvery { hubHandler.loadNotifications(20) } returns Result.success(listOf(notification(19), notification(20)))
        val vm = createViewModel()

        vm.setEvent(NotificationsState.Event.NotificationsLoadMoreSelected)

        assertEquals((0..20).toList(), vm.currentState.notifications.map { it.id })
        assertFalse(vm.currentState.hasMoreNotifications)
        assertFalse(vm.currentState.isNotificationsLoadingMore)
        coVerify(exactly = 1) { hubHandler.loadNotifications(20) }
    }

    @Test
    fun `failed load more keeps the list and allows a retry`() {
        stubNotifications(notifications = Result.success(List(20) { notification(it) }))
        coEvery { hubHandler.loadNotifications(20) } returns Result.failure(IllegalStateException("network"))
        val vm = createViewModel()

        vm.setEvent(NotificationsState.Event.NotificationsLoadMoreSelected)

        assertEquals(20, vm.currentState.notifications.size)
        assertTrue(vm.currentState.hasMoreNotifications)
        assertFalse(vm.currentState.isNotificationsLoadingMore)
        assertEquals(AccountUiError.LOAD_NOTIFICATIONS_FAILED, vm.currentState.error)
    }

    @Test
    fun `load more does nothing without a next page`() {
        stubNotifications(notifications = Result.success(listOf(notification(1))))
        val vm = createViewModel()

        vm.setEvent(NotificationsState.Event.NotificationsLoadMoreSelected)

        // Единственный запрос — первая страница при открытии экрана.
        coVerify(exactly = 1) { hubHandler.loadNotifications(any()) }
    }

    @Test
    fun `marking a notification read applies immediately and syncs the server`() {
        stubNotifications(
            notifications = Result.success(listOf(notification(1), notification(2, viewed = true))),
            counts = Result.success(listOf(NotificationCount("anime", 1))),
        )
        coEvery { notificationMutationHandler.markNotificationRead(1) } returns AccountNotificationMutationOutcome.Success
        val vm = createViewModel()

        vm.setEvent(NotificationsState.Event.NotificationReadSelected(1))

        assertTrue(vm.currentState.notifications.first { it.id == 1 }.viewed)
        assertEquals(0, vm.currentState.notificationCounts.single().count)
        coVerify(exactly = 1) { notificationMutationHandler.markNotificationRead(1) }
    }

    @Test
    fun `failed notification mutation reverts the list`() {
        stubNotifications(
            notifications = Result.success(listOf(notification(1))),
            counts = Result.success(listOf(NotificationCount("anime", 1))),
        )
        coEvery { notificationMutationHandler.deleteNotification(1) } returns
            AccountNotificationMutationOutcome.Failure(AccountUiError.UPDATE_NOTIFICATION_FAILED)
        val vm = createViewModel()

        vm.setEvent(NotificationsState.Event.NotificationDeleteSelected(1))

        assertEquals(listOf(1), vm.currentState.notifications.map { it.id })
        assertEquals(AccountUiError.UPDATE_NOTIFICATION_FAILED, vm.currentState.error)
    }

    @Test
    fun `new episode notification opens the title and marks itself read`() {
        stubNotifications(
            notifications = Result.success(listOf(notification(1, slug = "slug"))),
            counts = Result.success(listOf(NotificationCount("anime", 1))),
        )
        coEvery { notificationHandler.resolveAnimeId("slug") } returns AccountOpenNotificationResult.Navigate(55)
        coEvery { notificationMutationHandler.markNotificationRead(1) } returns AccountNotificationMutationOutcome.Success
        val vm = createViewModel()

        vm.setEvent(NotificationsState.Event.NotificationSelected(1))

        verify(exactly = 1) { detailsNavigator.getDetailsDest(55) }
        verify(exactly = 1) { nav.navigate(any()) }
        assertFalse(vm.currentState.isNotificationOpening)
        assertTrue(vm.currentState.notifications.single().viewed)
    }

    @Test
    fun `post comment notification opens the post comments and marks itself read`() {
        val comment = notification(1).copy(type = "comment", clickUri = "/posts/2516#comment9", subjectTitle = "Пост")
        stubNotifications(notifications = Result.success(listOf(comment)))
        coEvery { notificationMutationHandler.markNotificationRead(1) } returns AccountNotificationMutationOutcome.Success
        val vm = createViewModel()

        vm.setEvent(NotificationsState.Event.NotificationSelected(1))

        verify(exactly = 1) { commentsNavigator.getCommentsDest(CommentTargetType.POST, 2516, showDetailsAction = true) }
        verify(exactly = 1) { nav.navigate(any()) }
        assertTrue(vm.currentState.notifications.single().viewed)
    }

    @Test
    fun `anime comment notification opens the title comments by the id from the notification`() {
        val comment = notification(1, slug = "slug").copy(
            type = "comment",
            clickUri = "/catalog/item/slug#comment9",
            isNewEpisode = false,
            animeId = 77,
            subjectTitle = "Тайтл",
        )
        stubNotifications(notifications = Result.success(listOf(comment)))
        coEvery { notificationMutationHandler.markNotificationRead(1) } returns AccountNotificationMutationOutcome.Success
        val vm = createViewModel()

        vm.setEvent(NotificationsState.Event.NotificationSelected(1))

        verify(exactly = 1) {
            commentsNavigator.getAnimeCommentsDest(77, showDetailsAction = true)
        }
        verify(exactly = 0) { detailsNavigator.getDetailsDest(any()) }
        coVerify(exactly = 0) { notificationHandler.resolveAnimeId(any()) }
    }

    @Test
    fun `anime comment notification without an id is not openable`() {
        val comment = notification(1, slug = "slug").copy(
            type = "comment",
            clickUri = "/catalog/item/slug#comment9",
            isNewEpisode = false,
        )
        stubNotifications(notifications = Result.success(listOf(comment)))
        val vm = createViewModel()

        vm.setEvent(NotificationsState.Event.NotificationSelected(1))

        verify(exactly = 0) { nav.navigate(any()) }
        coVerify(exactly = 0) { notificationHandler.resolveAnimeId(any()) }
    }

    @Test
    fun `unresolvable notification shows the open error`() {
        stubNotifications(notifications = Result.success(listOf(notification(1, slug = "slug"))))
        coEvery { notificationHandler.resolveAnimeId(any()) } returns AccountOpenNotificationResult.Failure
        val vm = createViewModel()

        vm.setEvent(NotificationsState.Event.NotificationSelected(1))

        assertEquals(AccountUiError.OPEN_NOTIFICATION_FAILED, vm.currentState.error)
        verify(exactly = 0) { nav.navigate(any()) }
    }

    private companion object {
        const val USER_ID = 12
    }
}
