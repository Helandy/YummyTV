package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorEvent
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier

class DefaultAccountMutationErrorNotifier : AccountMutationErrorNotifier {
    private val mutableEvents = MutableSharedFlow<AccountMutationErrorEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val events: SharedFlow<AccountMutationErrorEvent> = mutableEvents.asSharedFlow()

    override suspend fun notify(event: AccountMutationErrorEvent) {
        mutableEvents.emit(event)
    }
}
