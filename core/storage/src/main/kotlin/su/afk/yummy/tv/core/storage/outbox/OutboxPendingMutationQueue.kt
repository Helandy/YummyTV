package su.afk.yummy.tv.core.storage.outbox

import su.afk.yummy.tv.core.model.error.isNetworkError
import su.afk.yummy.tv.core.model.mutation.PendingMutation
import su.afk.yummy.tv.core.model.mutation.PendingMutationQueue

/** [PendingMutationQueue] поверх [PendingMutationOutbox]: кодирует мутацию и запускает досылку. */
internal class OutboxPendingMutationQueue(
    private val outbox: PendingMutationOutbox,
    private val scheduler: PendingMutationSyncScheduler,
) : PendingMutationQueue {

    override suspend fun enqueueOnNetworkFailure(
        mutation: PendingMutation,
        error: Throwable,
    ): Boolean {
        if (!error.isNetworkError()) return false
        val (type, payload) = mutation.toTypeAndPayload()
        outbox.enqueue(type, payload)
        scheduler.scheduleFlush()
        return true
    }

    private fun PendingMutation.toTypeAndPayload(): Pair<String, String> = when (this) {
        is PendingMutation.MarkWatched ->
            PendingMutationTypes.MARK_WATCHED to
                MarkWatchedPayload(videoId, timeSeconds, durationSeconds).encode()

        is PendingMutation.RemoveWatched ->
            PendingMutationTypes.REMOVE_WATCHED to RemoveWatchedPayload(videoIds).encode()

        is PendingMutation.SetList ->
            PendingMutationTypes.SET_LIST to SetListPayload(animeId, listId).encode()

        is PendingMutation.RemoveList ->
            PendingMutationTypes.REMOVE_LIST to AnimeIdPayload(animeId).encode()

        is PendingMutation.SetFavorite ->
            PendingMutationTypes.SET_FAVORITE to SetFavoritePayload(animeId, favorite).encode()

        is PendingMutation.SetRating ->
            PendingMutationTypes.SET_RATING to SetRatingPayload(animeId, rating).encode()

        is PendingMutation.DeleteRating ->
            PendingMutationTypes.DELETE_RATING to AnimeIdPayload(animeId).encode()

        is PendingMutation.VoteReview ->
            PendingMutationTypes.VOTE_REVIEW to VoteReviewPayload(reviewId, voteApiValue).encode()
    }
}
