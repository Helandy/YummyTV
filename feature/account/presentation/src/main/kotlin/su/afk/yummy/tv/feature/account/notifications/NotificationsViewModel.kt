package su.afk.yummy.tv.feature.account.notifications

import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.comments.CommentTargetType
import su.afk.yummy.tv.core.model.error.toDisplayDetails
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.domain.account.model.NotificationCommentTarget
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.model.commentTarget
import su.afk.yummy.tv.domain.account.model.isExpandable
import su.afk.yummy.tv.domain.account.model.totalUnreadCount
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.account.account.AccountAnalytics
import su.afk.yummy.tv.feature.account.account.handler.AccountHubHandler
import su.afk.yummy.tv.feature.account.account.handler.AccountNotificationHandler
import su.afk.yummy.tv.feature.account.account.handler.AccountNotificationMutationHandler
import su.afk.yummy.tv.feature.account.account.handler.AccountNotificationMutationOutcome
import su.afk.yummy.tv.feature.account.account.handler.AccountOpenNotificationResult
import su.afk.yummy.tv.feature.account.account.handler.NOTIFICATIONS_PAGE_LIMIT
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.utils.decrementCount
import su.afk.yummy.tv.feature.comments.ICommentsNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import javax.inject.Inject

/**
 * Список уведомлений аккаунта: загрузка, подгрузка страниц, оптимистичные мутации и переходы из
 * уведомлений. Один на мобильный экран и ТВ-вкладку; счётчик непрочитанных для профиля отдаётся
 * через общую настройку [YaniAccountSettingsStore.yaniUnreadNotificationsCount].
 */
@HiltViewModel
class NotificationsViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val settingsStore: YaniAccountSettingsStore,
    private val observeAccountSession: ObserveAccountSessionUseCase,
    private val detailsNavigator: IDetailsNavigator,
    private val commentsNavigator: ICommentsNavigator,
    private val accountNavigator: IAccountNavigator,
    private val hubHandler: AccountHubHandler,
    private val notificationHandler: AccountNotificationHandler,
    private val notificationMutationHandler: AccountNotificationMutationHandler,
    private val analytics: AccountAnalytics,
) : BaseViewModel<NotificationsState.State, NotificationsState.Event, NotificationsState.Effect>() {

    override fun createInitialState() = NotificationsState.State()

    /** Пользователь, для которого список уже загружался; `0` — ещё нет или вышел из аккаунта. */
    private var loadedUserId = 0

    init {
        observeAccountSession()
            .onEach { session ->
                if (!session.isAuthorized || session.userId <= 0) {
                    loadedUserId = 0
                    setState { NotificationsState.State() }
                } else if (loadedUserId != session.userId) {
                    loadedUserId = session.userId
                    load()
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: NotificationsState.Event) {
        when (event) {
            NotificationsState.Event.BackSelected -> nav.back()
            // Возврат на экран: перечитываем, а нужен ли поход в сеть, решает репозиторий по TTL.
            NotificationsState.Event.ScreenShown -> if (loadedUserId > 0) load()
            NotificationsState.Event.RefreshSelected -> if (loadedUserId > 0) load()
            NotificationsState.Event.MySubscriptionsSelected ->
                nav.navigate(accountNavigator.getMySubscriptionsDest())

            is NotificationsState.Event.NotificationSelected -> openNotification(event.id)
            is NotificationsState.Event.NotificationExpandToggled -> toggleNotificationExpanded(event.id)
            is NotificationsState.Event.NotificationLinkSelected ->
                openNotificationLink(event.notificationId, event.slug)

            is NotificationsState.Event.NotificationReadSelected -> {
                analytics.eventNotificationReadSelected(event.id)
                markNotificationReadOptimistically(event.id)
            }

            NotificationsState.Event.AllNotificationsReadSelected -> {
                analytics.eventAllNotificationsReadSelected()
                markAllNotificationsReadOptimistically()
            }

            NotificationsState.Event.AllNotificationsDeleteSelected -> deleteAllNotificationsOptimistically()
            NotificationsState.Event.NotificationsLoadMoreSelected -> loadMoreNotifications()
            is NotificationsState.Event.NotificationDeleteSelected -> {
                analytics.eventNotificationDeleteSelected(event.id)
                deleteNotificationOptimistically(event.id)
            }
        }
    }

    override fun onRetry() {
        if (loadedUserId > 0) load()
    }

    private fun load() {
        viewModelScope.launch {
            setState { copy(isNotificationsLoading = true, error = null, errorDetails = null) }
            showCachedNotifications()
            val notifications = hubHandler.loadNotifications()
            val counts = hubHandler.loadNotificationCounts()
            applyNotifications(notifications)
            counts.onSuccess { list -> setState { copy(notificationCounts = list.toImmutableList()) } }
            setState { copy(isNotificationsLoading = false) }
        }
    }

    /** Показывает сохранённое, пусть и просроченное, чтобы экран не ждал сеть; свежее заменит его. */
    private suspend fun showCachedNotifications() {
        val cached = hubHandler.loadCachedNotifications() ?: return
        setState {
            if (notifications.isEmpty()) {
                copy(notifications = cached.distinctBy { it.id }.toImmutableList())
            } else {
                this
            }
        }
    }

    private fun applyNotifications(result: Result<List<ProfileNotification>>) {
        result.fold(
            onSuccess = { notifications ->
                setState {
                    copy(
                        notifications = notifications.distinctBy { it.id }.toImmutableList(),
                        hasMoreNotifications = notifications.size >= NOTIFICATIONS_PAGE_LIMIT,
                    )
                }
            },
            onFailure = { error ->
                val details = errorHandler.parse(error).toDisplayDetails()
                setState { copy(error = AccountUiError.LOAD_NOTIFICATIONS_FAILED, errorDetails = details) }
            },
        )
    }

    /** Подгружает одну следующую страницу; смещение — число уже загруженных, удалённые уходят и с сервера. */
    private fun loadMoreNotifications() {
        val state = currentState
        if (state.isNotificationsLoadingMore || !state.hasMoreNotifications) return
        setState { copy(isNotificationsLoadingMore = true) }
        viewModelScope.launch {
            hubHandler.loadNotifications(offset = state.notifications.size).fold(
                onSuccess = { page ->
                    setState {
                        copy(
                            notifications = (notifications + page).distinctBy { it.id }.toImmutableList(),
                            hasMoreNotifications = page.size >= NOTIFICATIONS_PAGE_LIMIT,
                            isNotificationsLoadingMore = false,
                        )
                    }
                },
                onFailure = { error ->
                    val details = errorHandler.parse(error).toDisplayDetails()
                    setState {
                        copy(
                            isNotificationsLoadingMore = false,
                            error = AccountUiError.LOAD_NOTIFICATIONS_FAILED,
                            errorDetails = details,
                        )
                    }
                },
            )
        }
    }

    private fun openNotification(id: Int) {
        val notification = currentState.notifications.firstOrNull { it.id == id } ?: return
        when (val target = notification.commentTarget) {
            null -> {
                if (!notification.isNewEpisode) return
                val slug = notification.animeSlug ?: return
                openNotificationAnime(notification, slug)
            }

            else -> openNotificationComments(notification, target)
        }
    }

    private fun openNotificationLink(notificationId: Int, slug: String) {
        val notification = currentState.notifications.firstOrNull { it.id == notificationId } ?: return
        openNotificationAnime(notification, slug)
    }

    private fun toggleNotificationExpanded(id: Int) {
        val notification = currentState.notifications.firstOrNull { it.id == id } ?: return
        if (!notification.isExpandable) return
        val expanding = id !in currentState.expandedNotificationIds
        setState {
            copy(
                expandedNotificationIds = if (expanding) {
                    expandedNotificationIds.adding(id)
                } else {
                    expandedNotificationIds.removing(id)
                },
            )
        }
        if (expanding && !notification.viewed) markNotificationReadOptimistically(id)
    }

    private fun openNotificationComments(
        notification: ProfileNotification,
        target: NotificationCommentTarget,
    ) {
        when (target) {
            is NotificationCommentTarget.Post -> openComments(
                notification,
                commentsNavigator.getCommentsDest(CommentTargetType.POST, target.postId, showDetailsAction = true),
            )

            is NotificationCommentTarget.Anime -> openComments(notification, animeCommentsDest(target.animeId))
        }
    }

    private fun animeCommentsDest(animeId: Int) =
        commentsNavigator.getAnimeCommentsDest(animeId, showDetailsAction = true)

    private fun openComments(notification: ProfileNotification, destination: NavKey) {
        if (!notification.viewed) markNotificationReadOptimistically(notification.id)
        nav.navigate(destination)
    }

    private fun openNotificationAnime(notification: ProfileNotification, slug: String) {
        if (currentState.isNotificationOpening) return
        viewModelScope.launch {
            setState { copy(isNotificationOpening = true, error = null) }
            when (val result = notificationHandler.resolveAnimeId(slug)) {
                is AccountOpenNotificationResult.Navigate -> {
                    analytics.eventNotificationSelected(notification, result.animeId)
                    setState { copy(isNotificationOpening = false) }
                    if (!notification.viewed) markNotificationReadOptimistically(notification.id)
                    nav.navigate(detailsNavigator.getDetailsDest(result.animeId))
                }

                AccountOpenNotificationResult.Failure -> {
                    setState {
                        copy(
                            isNotificationOpening = false,
                            error = AccountUiError.OPEN_NOTIFICATION_FAILED,
                        )
                    }
                }
            }
        }
    }

    // Мутации применяются к локальному состоянию сразу, чтобы UI не ждал сеть; запрос идёт в
    // фоне, и только неудача возвращает прежнее состояние: устаревший ответ от успеха неотличим.

    private fun markNotificationReadOptimistically(id: Int) {
        val notification = currentState.notifications.firstOrNull { it.id == id } ?: return
        if (notification.viewed) return
        mutateNotificationsOptimistically(
            notifications = currentState.notifications.map {
                if (it.id == id) it.copy(viewed = true) else it
            }.toImmutableList(),
            counts = currentState.notificationCounts.decrementCount(notification.type),
        ) { notificationMutationHandler.markNotificationRead(id) }
    }

    private fun deleteNotificationOptimistically(id: Int) {
        val notification = currentState.notifications.firstOrNull { it.id == id } ?: return
        mutateNotificationsOptimistically(
            notifications = currentState.notifications.filterNot { it.id == id }.toImmutableList(),
            counts = if (notification.viewed) {
                currentState.notificationCounts
            } else {
                currentState.notificationCounts.decrementCount(notification.type)
            },
            syncUnreadCount = !notification.viewed,
        ) { notificationMutationHandler.deleteNotification(id) }
    }

    private fun markAllNotificationsReadOptimistically() {
        if (currentState.notifications.all { it.viewed }) return
        mutateNotificationsOptimistically(
            notifications = currentState.notifications.map { it.copy(viewed = true) }.toImmutableList(),
            counts = currentState.notificationCounts.map { it.copy(count = 0) }.toImmutableList(),
        ) { notificationMutationHandler.markAllNotificationsRead() }
    }

    private fun deleteAllNotificationsOptimistically() {
        if (currentState.notifications.isEmpty()) return
        mutateNotificationsOptimistically(
            notifications = persistentListOf(),
            counts = persistentListOf(),
        ) { notificationMutationHandler.deleteAllNotifications() }
    }

    /** Применяет [notifications]/[counts] сразу, а при ошибке сервера возвращает прежнее состояние. */
    private fun mutateNotificationsOptimistically(
        notifications: ImmutableList<ProfileNotification>,
        counts: ImmutableList<NotificationCount>,
        syncUnreadCount: Boolean = true,
        mutation: suspend () -> AccountNotificationMutationOutcome,
    ) {
        val previousNotifications = currentState.notifications
        val previousCounts = currentState.notificationCounts
        setState { copy(notifications = notifications, notificationCounts = counts, error = null) }
        viewModelScope.launch {
            if (syncUnreadCount) {
                settingsStore.setYaniUnreadNotificationsCount(counts.totalUnreadCount())
            }
            val outcome = mutation()
            if (outcome is AccountNotificationMutationOutcome.Failure) {
                revertNotifications(previousNotifications, previousCounts, outcome.error)
            }
        }
    }

    private fun revertNotifications(
        notifications: ImmutableList<ProfileNotification>,
        counts: ImmutableList<NotificationCount>,
        error: AccountUiError,
    ) {
        setState { copy(notifications = notifications, notificationCounts = counts, error = error) }
        viewModelScope.launch {
            settingsStore.setYaniUnreadNotificationsCount(counts.totalUnreadCount())
        }
    }
}
