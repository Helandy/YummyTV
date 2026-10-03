package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.utils.notifyBooleanMutationFailure
import su.afk.yummy.tv.domain.account.model.AccountMutationAction
import su.afk.yummy.tv.domain.account.repository.AccountMutationErrorRepository
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository
import javax.inject.Inject

/** Помечает все уведомления профиля как прочитанные. */
class MarkAllNotificationsReadUseCase @Inject constructor(
    private val repository: ProfileNotificationsRepository,
    private val mutationErrorNotifier: AccountMutationErrorRepository,
) {
    suspend operator fun invoke() =
        notifyBooleanMutationFailure(
            mutationErrorNotifier,
            AccountMutationAction.MARK_ALL_NOTIFICATIONS_READ
        ) {
            repository.markAllNotificationsRead()
        }
}
