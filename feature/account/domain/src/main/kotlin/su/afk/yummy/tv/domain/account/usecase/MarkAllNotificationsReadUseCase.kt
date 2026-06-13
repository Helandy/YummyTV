package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.mutation.AccountMutationAction
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository
import javax.inject.Inject

/** Marks all profile notifications as read for the signed-in account. */
class MarkAllNotificationsReadUseCase @Inject constructor(
    private val repository: ProfileNotificationsRepository,
    private val mutationErrorNotifier: AccountMutationErrorNotifier,
) {
    suspend operator fun invoke() =
        notifyMutationFailure(
            mutationErrorNotifier,
            AccountMutationAction.MARK_ALL_NOTIFICATIONS_READ
        ) {
            repository.markAllNotificationsRead()
        }
}
