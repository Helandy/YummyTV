package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.mutation.AccountMutationAction
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.repository.VideoWatchesRepository
import javax.inject.Inject

/** Marks a video as watched with the latest playback timing. */
class MarkVideoWatchedUseCase @Inject constructor(
    private val repository: VideoWatchesRepository,
    private val mutationErrorNotifier: AccountMutationErrorNotifier,
) {
    suspend operator fun invoke(videoId: Int, timeSeconds: Int, durationSeconds: Int): Boolean =
        notifyMutationFailure(mutationErrorNotifier, AccountMutationAction.MARK_WATCHED) {
            repository.markWatched(videoId, timeSeconds, durationSeconds)
        }
}
