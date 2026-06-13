package su.afk.yummy.tv.feature.account

import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.account.usecase.DeleteNotificationUseCase
import su.afk.yummy.tv.domain.account.usecase.MarkAllNotificationsReadUseCase
import su.afk.yummy.tv.domain.account.usecase.MarkNotificationReadUseCase
import su.afk.yummy.tv.domain.account.usecase.ResolveNotificationAnimeIdUseCase
import javax.inject.Inject

/** Performs notification actions and resolves notification navigation targets. */
internal class AccountNotificationHandler @Inject constructor(
    private val settingsStore: SettingsStore,
    private val resolveNotificationAnimeId: ResolveNotificationAnimeIdUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase,
    private val markAllNotificationsReadUseCase: MarkAllNotificationsReadUseCase,
    private val deleteNotificationUseCase: DeleteNotificationUseCase,
) {
    suspend fun resolveAnimeId(slug: String): AccountOpenNotificationResult =
        runCatching { resolveNotificationAnimeId(slug) }.fold(
            onSuccess = { animeId ->
                animeId?.let(AccountOpenNotificationResult::Navigate)
                    ?: AccountOpenNotificationResult.Failure
            },
            onFailure = { AccountOpenNotificationResult.Failure },
        )

    suspend fun markNotificationRead(id: Int): Result<Boolean> =
        runCatching { markNotificationReadUseCase(id) }

    suspend fun deleteNotification(id: Int): Result<Boolean> =
        runCatching { deleteNotificationUseCase(id) }

    suspend fun markAllNotificationsRead(): Result<Boolean> =
        runCatching {
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
