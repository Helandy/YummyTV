package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.utils.notifyBooleanMutationFailure
import su.afk.yummy.tv.domain.account.model.AccountMutationAction
import su.afk.yummy.tv.domain.account.repository.AccountMutationErrorRepository
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository
import javax.inject.Inject

/** Удаляет одно уведомление профиля. */
class DeleteNotificationUseCase @Inject constructor(
    private val repository: ProfileNotificationsRepository,
    private val mutationErrorNotifier: AccountMutationErrorRepository,
) {
    suspend operator fun invoke(id: Int) =
        notifyBooleanMutationFailure(
            mutationErrorNotifier,
            AccountMutationAction.DELETE_NOTIFICATION
        ) {
            repository.deleteNotification(id)
        }
}
