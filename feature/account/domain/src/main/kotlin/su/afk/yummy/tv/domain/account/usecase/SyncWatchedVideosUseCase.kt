package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.model.RemoteWatchState
import su.afk.yummy.tv.domain.account.mutation.AccountMutationAction
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.repository.VideoWatchesRepository

/** Syncs a batch of remote watch states with the account API. */
class SyncWatchedVideosUseCase(
    private val repository: VideoWatchesRepository,
    private val mutationErrorNotifier: AccountMutationErrorNotifier,
) {
    suspend operator fun invoke(states: List<RemoteWatchState>): Boolean =
        notifyMutationFailure(mutationErrorNotifier, AccountMutationAction.SYNC_WATCHED) {
            repository.syncWatched(states)
        }
}
