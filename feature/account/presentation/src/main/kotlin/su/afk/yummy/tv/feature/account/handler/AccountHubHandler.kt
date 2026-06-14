package su.afk.yummy.tv.feature.account.handler

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.usecase.GetNotificationCountsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetProfileNotificationsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserProfileSummaryUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserStatsUseCase
import su.afk.yummy.tv.feature.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.utils.totalUnreadCount
import javax.inject.Inject

/** Loads account hub data and keeps unread notification count preferences in sync. */
internal class AccountHubHandler @Inject constructor(
    private val settingsStore: SettingsStore,
    private val getUserProfileSummary: GetUserProfileSummaryUseCase,
    private val getUserStats: GetUserStatsUseCase,
    private val getNotifications: GetProfileNotificationsUseCase,
    private val getNotificationCounts: GetNotificationCountsUseCase,
) {
    suspend fun loadHub(userId: Int): AccountHubLoadResult = coroutineScope {
        val profileSummaryDeferred = async { runCatching { getUserProfileSummary(userId) } }
        val statsDeferred = async { runCatching { getUserStats(userId) } }
        val notificationsDeferred = async { loadNotifications() }

        val profileSummaryResult = profileSummaryDeferred.await()
        val statsResult = statsDeferred.await()
        val notificationsResult = notificationsDeferred.await()
        AccountHubLoadResult(
            profileSummary = profileSummaryResult.getOrNull(),
            profileSummaryError = if (profileSummaryResult.isFailure) {
                AccountUiError.LOAD_PROFILE_STATISTICS_FAILED
            } else {
                null
            },
            stats = statsResult.getOrNull(),
            statsError = if (statsResult.isFailure) {
                AccountUiError.LOAD_PROFILE_STATISTICS_FAILED
            } else {
                null
            },
            notifications = notificationsResult,
        )
    }

    suspend fun loadNotifications(): AccountNotificationsLoadResult =
        runCatching {
            getNotifications(limit = 20) to getNotificationCounts()
        }.fold(
            onSuccess = { (notifications, counts) ->
                settingsStore.setYaniUnreadNotificationsCount(counts.totalUnreadCount())
                AccountNotificationsLoadResult.Success(
                    notifications = notifications,
                    counts = counts,
                )
            },
            onFailure = {
                AccountNotificationsLoadResult.Failure(AccountUiError.LOAD_NOTIFICATIONS_FAILED)
            },
        )
}

/** Combined account hub payload with independent stats and notification outcomes. */
internal data class AccountHubLoadResult(
    val profileSummary: UserProfileSummary?,
    val profileSummaryError: AccountUiError?,
    val stats: UserStats?,
    val statsError: AccountUiError?,
    val notifications: AccountNotificationsLoadResult,
)

/** Result of loading profile notifications and their unread counters. */
internal sealed interface AccountNotificationsLoadResult {
    data class Success(
        val notifications: List<ProfileNotification>,
        val counts: List<NotificationCount>,
    ) : AccountNotificationsLoadResult

    data class Failure(val error: AccountUiError) : AccountNotificationsLoadResult
}
