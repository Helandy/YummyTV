package su.afk.yummy.tv.domain.account.mutation

import kotlinx.coroutines.flow.SharedFlow

interface AccountMutationErrorNotifier {
    val events: SharedFlow<AccountMutationErrorEvent>

    suspend fun notify(event: AccountMutationErrorEvent)
}
