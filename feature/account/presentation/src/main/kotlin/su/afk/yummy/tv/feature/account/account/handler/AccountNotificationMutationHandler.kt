package su.afk.yummy.tv.feature.account.account.handler

import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import javax.inject.Inject

internal class AccountNotificationMutationHandler @Inject constructor(
    private val hubHandler: AccountHubHandler,
    private val notificationHandler: AccountNotificationHandler,
) {
    suspend fun markNotificationRead(id: Int): AccountNotificationMutationResult =
        mutate(
            error = AccountUiError.UPDATE_NOTIFICATION_FAILED,
            action = { notificationHandler.markNotificationRead(id) },
        )

    suspend fun deleteNotification(id: Int): AccountNotificationMutationResult =
        mutate(
            error = AccountUiError.UPDATE_NOTIFICATION_FAILED,
            action = { notificationHandler.deleteNotification(id) },
        )

    suspend fun deleteAllNotifications(): AccountNotificationMutationResult =
        mutate(
            error = AccountUiError.UPDATE_NOTIFICATIONS_FAILED,
            action = notificationHandler::deleteAllNotifications,
        )

    suspend fun markAllNotificationsRead(): AccountNotificationMutationResult =
        mutate(
            error = AccountUiError.UPDATE_NOTIFICATIONS_FAILED,
            action = { notificationHandler.markAllNotificationsRead() },
        )

    private suspend fun mutate(
        error: AccountUiError,
        action: suspend () -> Result<Boolean>,
    ): AccountNotificationMutationResult =
        action().fold(
            onSuccess = { updated ->
                if (updated) {
                    AccountNotificationMutationResult.Reloaded(hubHandler.loadNotifications())
                } else {
                    AccountNotificationMutationResult.Unchanged
                }
            },
            onFailure = { AccountNotificationMutationResult.Failure(error) },
        )
}

internal sealed interface AccountNotificationMutationResult {
    data object Unchanged : AccountNotificationMutationResult
    data class Reloaded(val notifications: AccountNotificationsLoadResult) :
        AccountNotificationMutationResult

    data class Failure(val error: AccountUiError) : AccountNotificationMutationResult
}
