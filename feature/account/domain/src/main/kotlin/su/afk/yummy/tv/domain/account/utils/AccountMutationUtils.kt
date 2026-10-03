package su.afk.yummy.tv.domain.account.utils

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import su.afk.yummy.tv.domain.account.model.AccountMutationAction
import su.afk.yummy.tv.domain.account.model.AccountMutationErrorEvent
import su.afk.yummy.tv.domain.account.repository.AccountMutationErrorRepository

internal suspend inline fun <T> notifyMutationFailure(
    notifier: AccountMutationErrorRepository,
    action: AccountMutationAction,
    block: suspend () -> T,
): T = try {
    block()
} catch (error: Throwable) {
    currentCoroutineContext().ensureActive()
    notifier.notify(AccountMutationErrorEvent(action = action, message = error.message))
    throw error
}

internal suspend inline fun notifyBooleanMutationFailure(
    notifier: AccountMutationErrorRepository,
    action: AccountMutationAction,
    block: suspend () -> Boolean,
): Boolean = notifyMutationFailure(notifier, action) {
    block().also { success ->
        if (!success) {
            notifier.notify(AccountMutationErrorEvent(action = action, message = null))
        }
    }
}
