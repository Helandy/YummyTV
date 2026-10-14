package su.afk.yummy.tv.android.outbox

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.model.error.isNetworkError
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

    override suspend fun doWork(): Result = flushLock.withLock { flush() }

    private suspend fun flush(): Result {
        // Очередь — лог «последняя запись побеждает» (SET_RATING → DELETE_RATING для одного аниме),
        // поэтому на первой повторяемой ошибке останавливаемся: пропуск упавшей записи позволил бы
        // старой мутации переиграться поверх более новых в следующем прогоне.
        for (entry in outbox.pending()) {
            val applied = runSuspendCatching { apply(entry) }
            val error = applied.exceptionOrNull()
            when {
                applied.isSuccess -> outbox.remove(entry.id)

                // Сервер однозначно отверг мутацию (4xx) — повтор ничего не изменит.
                error.isPermanentRejection() -> outbox.remove(entry.id)

                // Всё ещё офлайн — оставляем запись без траты попыток (офлайн может длиться днями).
                error?.isNetworkError() == true -> return Result.retry()

                // Сервер долго отвечает ошибкой (5xx, мусор вместо JSON и т.п.) — не копим вечно.
                entry.attemptCount + 1 >= MAX_ATTEMPTS -> outbox.remove(entry.id)

                // 5xx, 429 или неразобранный ответ: оставляем запись и повторим в следующем прогоне,
                // иначе оценка/список/«просмотрено» теряются молча.
                else -> {
                    outbox.recordAttemptFailure(entry.id)
                    return Result.retry()
                }
            }
        }
        return Result.success()
    }

    /** 4xx, кроме «повторить позже» (408/425/429): запрос заведомо не пройдёт и при повторе. */
    private fun Throwable?.isPermanentRejection(): Boolean {
        val status = (this as? ResponseException)?.response?.status?.value ?: return false
        return status in 400..499 && status != 408 && status != 425 && status != 429
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

    private companion object {
        const val MAX_ATTEMPTS = 8

        /**
         * Периодический и одноразовый воркеры — разные work-имена и могут стартовать одновременно;
         * без замка они переигрывали бы одни и те же записи, нарушая порядок «последняя запись побеждает».
         */
        val flushLock = Mutex()
    }
}
