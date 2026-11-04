package su.afk.yummy.tv.feature.account.account.handler

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.model.totalUnreadCount
import su.afk.yummy.tv.domain.account.usecase.GetCachedProfileNotificationsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetCachedUserProfileSummaryUseCase
import su.afk.yummy.tv.domain.account.usecase.GetCachedUserStatsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetNotificationCountsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetProfileNotificationsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserProfileSummaryUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserStatsUseCase
import javax.inject.Inject

internal const val NOTIFICATIONS_PAGE_LIMIT = 20

/**
 * Loads account hub data in independent parts so each one can be shown as soon as it arrives,
 * and keeps unread notification count preferences in sync.
 */
internal class AccountHubHandler @Inject constructor(
    private val settingsStore: YaniAccountSettingsStore,
    private val getUserProfileSummary: GetUserProfileSummaryUseCase,
    private val getUserStats: GetUserStatsUseCase,
    private val getNotifications: GetProfileNotificationsUseCase,
    private val getNotificationCounts: GetNotificationCountsUseCase,
    private val getCachedUserProfileSummary: GetCachedUserProfileSummaryUseCase,
    private val getCachedUserStats: GetCachedUserStatsUseCase,
    private val getCachedNotifications: GetCachedProfileNotificationsUseCase,
) {
    /** Reads whatever the local cache holds, of any age, so the screen has content before the network answers. */
    suspend fun loadCached(userId: Int): AccountCachedHub = coroutineScope {
        val profileSummary = async { runSuspendCatching { getCachedUserProfileSummary(userId) }.getOrNull() }
        val stats = async { runSuspendCatching { getCachedUserStats(userId) }.getOrNull() }
        val notifications = async {
            runSuspendCatching { getCachedNotifications(NOTIFICATIONS_PAGE_LIMIT) }.getOrNull()
        }
        AccountCachedHub(
            profileSummary = profileSummary.await(),
            stats = stats.await(),
            notifications = notifications.await(),
        )
    }

    suspend fun loadProfileSummary(userId: Int): Result<UserProfileSummary> =
        runSuspendCatching { getUserProfileSummary(userId) }

    suspend fun loadStats(userId: Int): Result<UserStats> =
        runSuspendCatching { getUserStats(userId) }

    suspend fun loadNotifications(offset: Int = 0): Result<List<ProfileNotification>> =
        runSuspendCatching { getNotifications(limit = NOTIFICATIONS_PAGE_LIMIT, offset = offset) }

    /** Counts are derived from the full notification list, so they are loaded apart from the first page. */
    suspend fun loadNotificationCounts(): Result<List<NotificationCount>> =
        runSuspendCatching {
            getNotificationCounts().also { settingsStore.setYaniUnreadNotificationsCount(it.totalUnreadCount()) }
        }
}

/** Hub data found in the local cache; a part is `null` when nothing is stored for it. */
internal data class AccountCachedHub(
    val profileSummary: UserProfileSummary?,
    val stats: UserStats?,
    val notifications: List<ProfileNotification>?,
)
