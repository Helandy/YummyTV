package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.mutation.AccountMutationAction
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository
import javax.inject.Inject

/** Удаляет одно уведомление профиля. */
class DeleteNotificationUseCase @Inject constructor(
    private val repository: ProfileNotificationsRepository,
    private val mutationErrorNotifier: AccountMutationErrorNotifier,
) {
    suspend operator fun invoke(id: Int) =
        notifyMutationFailure(mutationErrorNotifier, AccountMutationAction.DELETE_NOTIFICATION) {
            repository.deleteNotification(id)
        }
}
