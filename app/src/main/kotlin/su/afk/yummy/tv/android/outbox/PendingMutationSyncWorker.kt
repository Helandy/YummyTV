package su.afk.yummy.tv.android.outbox

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import su.afk.yummy.tv.core.error.api.isNetworkError
import su.afk.yummy.tv.core.storage.outbox.AnimeIdPayload
import su.afk.yummy.tv.core.storage.outbox.MarkWatchedPayload
import su.afk.yummy.tv.core.storage.outbox.PendingMutationEntry
import su.afk.yummy.tv.core.storage.outbox.PendingMutationOutbox
import su.afk.yummy.tv.core.storage.outbox.PendingMutationTypes
import su.afk.yummy.tv.core.storage.outbox.RemoveWatchedPayload
import su.afk.yummy.tv.core.storage.outbox.SetFavoritePayload
import su.afk.yummy.tv.core.storage.outbox.SetListPayload
import su.afk.yummy.tv.core.storage.outbox.SetRatingPayload
import su.afk.yummy.tv.core.storage.outbox.VoteReviewPayload
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.usecase.DeleteAnimeRatingUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveWatchedVideosUseCase
import su.afk.yummy.tv.domain.account.usecase.SaveVideoWatchProgressUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeFavoriteUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeRatingUseCase
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.domain.reviews.usecase.VoteReviewUseCase

/**
 * Дожимает мутации из [PendingMutationOutbox], поставленные в очередь presentation-хендлерами при
 * сетевом сбое (см. `EpisodeWatchedHandler`, `DetailsLibraryHandler`, `RatingMutationHandler`,
 * `ReviewsListViewModel`/`ReviewDetailsViewModel`). Живёт в `:app`, а не в `core`, потому что ему
 * нужен доступ к use case'ам сразу нескольких фич — так делает и [su.afk.yummy.tv.android.episodepush.NewEpisodePushWorker].
 */
@HiltWorker
class PendingMutationSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val outbox: PendingMutationOutbox,
    private val saveVideoWatchProgress: SaveVideoWatchProgressUseCase,
    private val removeWatchedVideos: RemoveWatchedVideosUseCase,
    private val setAnimeList: SetAnimeListUseCase,
    private val removeAnimeList: RemoveAnimeListUseCase,
    private val setAnimeFavorite: SetAnimeFavoriteUseCase,
    private val setAnimeRating: SetAnimeRatingUseCase,
    private val deleteAnimeRating: DeleteAnimeRatingUseCase,
    private val voteReview: VoteReviewUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        var stillPending = false
        outbox.pending().forEach { entry ->
            val applied = runCatching { apply(entry) }
            if (applied.isSuccess) {
                outbox.remove(entry.id)
            } else {
                val error = applied.exceptionOrNull()
                if (error?.isNetworkError() == true) {
                    // Всё ещё офлайн (или сеть моргнула повторно) — оставляем запись, повторим
                    // в следующем прогоне вместо того, чтобы копить дубликаты.
                    stillPending = true
                } else {
                    // Сервер отверг мутацию (не сетевая причина) — повторять бессмысленно.
                    outbox.remove(entry.id)
                }
            }
        }
        return if (stillPending) Result.retry() else Result.success()
    }

    private suspend fun apply(entry: PendingMutationEntry) {
        when (entry.type) {
            PendingMutationTypes.MARK_WATCHED -> {
                val payload = MarkWatchedPayload.decode(entry.payloadJson)
                saveVideoWatchProgress(
                    videoId = payload.videoId,
                    timeSeconds = payload.timeSeconds,
                    durationSeconds = payload.durationSeconds,
                    times = emptyList(),
                )
            }

            PendingMutationTypes.REMOVE_WATCHED ->
                removeWatchedVideos(RemoveWatchedPayload.decode(entry.payloadJson).videoIds)

            PendingMutationTypes.SET_LIST -> {
                val payload = SetListPayload.decode(entry.payloadJson)
                val list = UserAnimeList.entries.first { it.id == payload.listId }
                setAnimeList(payload.animeId, list)
            }

            PendingMutationTypes.REMOVE_LIST ->
                removeAnimeList(AnimeIdPayload.decode(entry.payloadJson).animeId)

            PendingMutationTypes.SET_FAVORITE -> {
                val payload = SetFavoritePayload.decode(entry.payloadJson)
                setAnimeFavorite(payload.animeId, payload.favorite)
            }

            PendingMutationTypes.SET_RATING -> {
                val payload = SetRatingPayload.decode(entry.payloadJson)
                setAnimeRating(payload.animeId, payload.rating)
            }

            PendingMutationTypes.DELETE_RATING ->
                deleteAnimeRating(AnimeIdPayload.decode(entry.payloadJson).animeId)

            PendingMutationTypes.VOTE_REVIEW -> {
                val payload = VoteReviewPayload.decode(entry.payloadJson)
                val vote = ReviewVote.entries.first { it.apiValue == payload.voteApiValue }
                voteReview(payload.reviewId, vote)
            }

            else -> Unit
        }
    }
}
