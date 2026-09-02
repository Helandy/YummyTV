package su.afk.yummy.tv.feature.details.rating.handler

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import su.afk.yummy.tv.core.error.api.isNetworkError
import su.afk.yummy.tv.core.storage.outbox.AnimeIdPayload
import su.afk.yummy.tv.core.storage.outbox.PendingMutationOutbox
import su.afk.yummy.tv.core.storage.outbox.PendingMutationSyncScheduler
import su.afk.yummy.tv.core.storage.outbox.PendingMutationTypes
import su.afk.yummy.tv.core.storage.outbox.SetRatingPayload
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.account.model.AnimeListStats
import su.afk.yummy.tv.domain.account.model.AnimeRatingSummary
import su.afk.yummy.tv.domain.account.usecase.DeleteAnimeRatingUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeListStatsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeRatingSummaryUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeUserRatingUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeRatingUseCase
import javax.inject.Inject

/** Loads and mutates a title's rating, keeping fetch/mutation side-effects out of the ViewModel. */
internal class RatingMutationHandler @Inject constructor(
    private val getAnimeRatingSummary: GetAnimeRatingSummaryUseCase,
    private val getAnimeListStats: GetAnimeListStatsUseCase,
    private val getAnimeUserRating: GetAnimeUserRatingUseCase,
    private val setAnimeRating: SetAnimeRatingUseCase,
    private val deleteAnimeRating: DeleteAnimeRatingUseCase,
    private val pendingMutationOutbox: PendingMutationOutbox,
    private val pendingMutationSyncScheduler: PendingMutationSyncScheduler,
) {
    suspend fun load(animeId: Int): RatingLoadResult = coroutineScope {
        val ratingSummary = async { runSuspendCatching { getAnimeRatingSummary(animeId) } }
        val listStats = async { runSuspendCatching { getAnimeListStats(animeId) } }
        val userRating = async { runSuspendCatching { getAnimeUserRating(animeId) } }
        RatingLoadResult(
            ratingSummary = ratingSummary.await(),
            listStats = listStats.await(),
            userRating = userRating.await(),
        )
    }

    suspend fun setRating(animeId: Int, rating: Int): RatingMutationResult =
        runCatching { setAnimeRating(animeId, rating) }
            .toMutationResult(
                PendingMutationTypes.SET_RATING,
                SetRatingPayload(animeId, rating).encode()
            )

    suspend fun deleteRating(animeId: Int): RatingMutationResult =
        runCatching { deleteAnimeRating(animeId) }
            .toMutationResult(PendingMutationTypes.DELETE_RATING, AnimeIdPayload(animeId).encode())

    suspend fun refreshSummary(animeId: Int): AnimeRatingSummary? =
        runCatching { getAnimeRatingSummary(animeId) }.getOrNull()

    /**
     * Сетевой сбой ставится в offline-очередь и считается успехом — мутация дойдёт до сервера,
     * как только вернётся сеть. Остальные ошибки остаются провалом.
     */
    private suspend fun Result<*>.toMutationResult(
        type: String,
        payloadJson: String
    ): RatingMutationResult {
        if (isSuccess) return RatingMutationResult.Success
        val error = exceptionOrNull()
        if (error != null && error.isNetworkError()) {
            pendingMutationOutbox.enqueue(type, payloadJson)
            pendingMutationSyncScheduler.scheduleFlush()
            return RatingMutationResult.Success
        }
        return RatingMutationResult.Failure
    }
}

/** Partial-success outcome of loading rating summary, list stats and the current user rating. */
internal data class RatingLoadResult(
    val ratingSummary: Result<AnimeRatingSummary>,
    val listStats: Result<AnimeListStats>,
    val userRating: Result<Int?>,
)

/** Outcome of a rating mutation (set or delete). */
internal sealed interface RatingMutationResult {
    data object Success : RatingMutationResult
    data object Failure : RatingMutationResult
}
