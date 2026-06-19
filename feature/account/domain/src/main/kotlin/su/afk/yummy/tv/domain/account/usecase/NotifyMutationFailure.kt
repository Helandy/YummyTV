package su.afk.yummy.tv.domain.account.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.domain.account.mutation.AccountMutationAction
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorEvent
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier

internal suspend inline fun <T> notifyMutationFailure(
    notifier: AccountMutationErrorNotifier,
    action: AccountMutationAction,
    block: suspend () -> T,
): T = try {
    block()
} catch (error: Throwable) {
    if (error is CancellationException) throw error
    notifier.notify(AccountMutationErrorEvent(action = action, message = error.message))
    throw error
}

internal suspend inline fun notifyBooleanMutationFailure(
    notifier: AccountMutationErrorNotifier,
    action: AccountMutationAction,
    block: suspend () -> Boolean,
): Boolean = notifyMutationFailure(notifier, action) {
    block().also { success ->
        if (!success) {
            notifier.notify(AccountMutationErrorEvent(action = action, message = null))
        }
    }
}
