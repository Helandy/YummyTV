package su.afk.yummy.tv.domain.account.repository

import kotlinx.coroutines.flow.SharedFlow
import su.afk.yummy.tv.domain.account.model.AccountMutationErrorEvent

interface AccountMutationErrorRepository {
    val events: SharedFlow<AccountMutationErrorEvent>

    suspend fun notify(event: AccountMutationErrorEvent)
}
