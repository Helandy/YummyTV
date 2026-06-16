package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.mutation.AccountMutationAction
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.repository.VideoWatchesRepository
import javax.inject.Inject

/** Removes watched markers for remote videos. */
class RemoveWatchedVideosUseCase @Inject constructor(
    private val repository: VideoWatchesRepository,
    private val mutationErrorNotifier: AccountMutationErrorNotifier,
) {
    suspend operator fun invoke(videoIds: List<Int>): Boolean {
        val ids = videoIds.filter { it > 0 }.distinct()
        if (ids.isEmpty()) return true
        return notifyMutationFailure(mutationErrorNotifier, AccountMutationAction.REMOVE_WATCHED) {
            repository.removeWatched(ids)
        }
    }
}
