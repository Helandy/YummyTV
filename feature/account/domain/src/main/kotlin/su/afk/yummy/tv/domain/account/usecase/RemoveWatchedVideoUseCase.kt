package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.mutation.AccountMutationAction
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.repository.VideoWatchesRepository
import javax.inject.Inject

/** Removes the watched marker for a remote video. */
class RemoveWatchedVideoUseCase @Inject constructor(
    private val repository: VideoWatchesRepository,
    private val mutationErrorNotifier: AccountMutationErrorNotifier,
) {
    suspend operator fun invoke(videoId: Int): Boolean =
        notifyMutationFailure(mutationErrorNotifier, AccountMutationAction.REMOVE_WATCHED) {
            repository.removeWatched(videoId)
        }
}
