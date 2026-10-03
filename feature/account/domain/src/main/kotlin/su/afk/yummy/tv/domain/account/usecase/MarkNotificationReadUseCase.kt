package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.utils.notifyBooleanMutationFailure
import su.afk.yummy.tv.domain.account.model.AccountMutationAction
import su.afk.yummy.tv.domain.account.repository.AccountMutationErrorRepository
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository
import javax.inject.Inject

/** Помечает одно уведомление профиля как прочитанное. */
class MarkNotificationReadUseCase @Inject constructor(
    private val repository: ProfileNotificationsRepository,
    private val mutationErrorNotifier: AccountMutationErrorRepository,
) {
    suspend operator fun invoke(id: Int) =
        notifyBooleanMutationFailure(
            mutationErrorNotifier,
            AccountMutationAction.MARK_NOTIFICATION_READ
        ) {
            repository.markNotificationRead(id)
        }
}
