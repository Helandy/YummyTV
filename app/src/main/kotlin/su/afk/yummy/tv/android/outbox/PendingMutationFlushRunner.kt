package su.afk.yummy.tv.android.outbox

import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.model.error.isNetworkError
import su.afk.yummy.tv.core.model.mutation.PendingMutationFlusher
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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Дожимает мутации из [PendingMutationOutbox], поставленные в очередь presentation-хендлерами при
 * сетевом сбое (см. `EpisodeWatchedHandler`, `DetailsLibraryHandler`, `RatingMutationHandler`,
 * `ReviewsListViewModel`/`ReviewDetailsViewModel`). Живёт в `:app`, а не в `core`, потому что ему
 * нужен доступ к use case'ам сразу нескольких фич — так делает и [su.afk.yummy.tv.android.episodepush.NewEpisodePushWorker].
 * Общий для воркера и выхода из аккаунта: оба прогона разводит один замок.
 */
@Singleton
class PendingMutationFlushRunner @Inject constructor(
    private val outbox: PendingMutationOutbox,
    private val saveVideoWatchProgress: SaveVideoWatchProgressUseCase,
    private val removeWatchedVideos: RemoveWatchedVideosUseCase,
    private val setAnimeList: SetAnimeListUseCase,
    private val removeAnimeList: RemoveAnimeListUseCase,
    private val setAnimeFavorite: SetAnimeFavoriteUseCase,
    private val setAnimeRating: SetAnimeRatingUseCase,
    private val deleteAnimeRating: DeleteAnimeRatingUseCase,
    private val voteReview: VoteReviewUseCase,
) : PendingMutationFlusher {

    override suspend fun flush(): Boolean = flushLock.withLock { flushLocked() }

    private suspend fun flushLocked(): Boolean {
        // Очередь — лог «последняя запись побеждает» (SET_RATING → DELETE_RATING для одного аниме),
        // поэтому на первой повторяемой ошибке останавливаемся: пропуск упавшей записи позволил бы
        // старой мутации переиграться поверх более новых в следующем прогоне.
        for (entry in outbox.pending()) {
            val applied = runSuspendCatching { apply(entry) }
            val error = applied.exceptionOrNull()
            when {
                applied.isSuccess -> outbox.remove(entry.id)

                // Сервер однозначно отверг мутацию (4xx, кроме 401) — повтор ничего не изменит.
                error.isPermanentRejection() -> outbox.remove(entry.id)

                // Всё ещё офлайн — оставляем запись без траты попыток (офлайн может длиться днями).
                error?.isNetworkError() == true -> return false

                // Сервер долго отвечает ошибкой (5xx, мусор вместо JSON и т.п.) — не копим вечно.
                entry.attemptCount + 1 >= MAX_ATTEMPTS -> outbox.remove(entry.id)

                // 5xx, 429 или неразобранный ответ: оставляем запись и повторим в следующем прогоне,
                // иначе оценка/список/«просмотрено» теряются молча.
                else -> {
                    outbox.recordAttemptFailure(entry.id)
                    return false
                }
            }
        }
        return true
    }

    /**
     * 4xx, кроме «повторить позже» (408/425/429) и 401: запрос заведомо не пройдёт и при повторе.
     * 401 — не отказ по сути мутации, а проблема сессии (токен ещё не подхвачен, сбой авторизации
     * на стороне сервера): запись оставляем и считаем попытку, чтобы не терять оценку или список.
     * При выходе из аккаунта очередь чистится целиком, так что чужим токеном она не уйдёт.
     */
    private fun Throwable?.isPermanentRejection(): Boolean {
        val status = (this as? ResponseException)?.response?.status?.value ?: return false
        return status in 400..499 && status !in RETRYABLE_CLIENT_STATUSES
    }

    private suspend fun apply(entry: PendingMutationEntry) {
        when (entry.type) {
            PendingMutationTypes.MARK_WATCHED -> {
                val payload = MarkWatchedPayload.decode(entry.payloadJson)
                saveVideoWatchProgress(
                    videoId = payload.videoId,
                    timeSeconds = payload.timeSeconds,
                    durationSeconds = payload.durationSeconds,
                    times = payload.times,
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
        val RETRYABLE_CLIENT_STATUSES = setOf(401, 408, 425, 429)

        /**
         * Периодический и одноразовый воркеры — разные work-имена и могут стартовать одновременно;
         * без замка они переигрывали бы одни и те же записи, нарушая порядок «последняя запись побеждает».
         */
        val flushLock = Mutex()
    }
}
