package su.afk.yummy.tv.feature.account.account.handler

import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.account.usecase.DeleteAllNotificationsUseCase
import su.afk.yummy.tv.domain.account.usecase.DeleteNotificationUseCase
import su.afk.yummy.tv.domain.account.usecase.MarkAllNotificationsReadUseCase
import su.afk.yummy.tv.domain.account.usecase.MarkNotificationReadUseCase
import su.afk.yummy.tv.domain.account.usecase.ResolveNotificationAnimeIdUseCase
import javax.inject.Inject

/** Performs notification actions and resolves notification navigation targets. */
internal class AccountNotificationHandler @Inject constructor(
    private val settingsStore: YaniAccountSettingsStore,
    private val resolveNotificationAnimeId: ResolveNotificationAnimeIdUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase,
    private val markAllNotificationsReadUseCase: MarkAllNotificationsReadUseCase,
    private val deleteNotificationUseCase: DeleteNotificationUseCase,
    private val deleteAllNotificationsUseCase: DeleteAllNotificationsUseCase,
) {
    suspend fun resolveAnimeId(slug: String): AccountOpenNotificationResult =
        runSuspendCatching { resolveNotificationAnimeId(slug) }.fold(
            onSuccess = { animeId ->
                animeId?.let(AccountOpenNotificationResult::Navigate)
                    ?: AccountOpenNotificationResult.Failure
            },
            onFailure = { AccountOpenNotificationResult.Failure },
        )

    suspend fun markNotificationRead(id: Int): Result<Boolean> =
        runSuspendCatching { markNotificationReadUseCase(id) }

    suspend fun deleteNotification(id: Int): Result<Boolean> =
        runSuspendCatching { deleteNotificationUseCase(id) }

    suspend fun deleteAllNotifications(): Result<Boolean> =
        runSuspendCatching {
            val deleted = deleteAllNotificationsUseCase()
            if (deleted) settingsStore.setYaniUnreadNotificationsCount(0)
            deleted
        }

    suspend fun markAllNotificationsRead(): Result<Boolean> =
        runSuspendCatching {
            val updated = markAllNotificationsReadUseCase()
            if (updated) settingsStore.setYaniUnreadNotificationsCount(0)
            updated
        }
}

/** Result of resolving a notification into an anime details destination. */
internal sealed interface AccountOpenNotificationResult {
    data class Navigate(val animeId: Int) : AccountOpenNotificationResult
    data object Failure : AccountOpenNotificationResult
}
