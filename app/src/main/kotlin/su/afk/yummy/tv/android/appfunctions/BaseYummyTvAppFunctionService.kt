package su.afk.yummy.tv.android.appfunctions

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.core.net.toUri
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.android.InterfaceRouterActivity
import su.afk.yummy.tv.android.appfunctions.model.AnimeDetailsResult
import su.afk.yummy.tv.android.appfunctions.model.AnimeMatch
import su.afk.yummy.tv.android.appfunctions.model.AnimeScheduleDayResult
import su.afk.yummy.tv.android.appfunctions.model.AnimeScheduleEntry
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.catalog.usecase.SearchUseCase
import su.afk.yummy.tv.domain.home.model.hasPlayableTarget
import su.afk.yummy.tv.domain.home.usecase.ObserveContinueWatchingUseCase
import su.afk.yummy.tv.domain.schedule.usecase.GetAnimeScheduleUseCase
import javax.inject.Inject

@RequiresApi(36)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "YummyTvAppFunctionService",
    appFunctionXmlFileName = "yummy_tv_app_functions",
)
abstract class BaseYummyTvAppFunctionService : AppFunctionService() {

    @Inject
    internal lateinit var search: SearchUseCase

    @Inject
    internal lateinit var getDetails: GetAnimeDetailsUseCase

    @Inject
    internal lateinit var getSchedule: GetAnimeScheduleUseCase

    @Inject
    internal lateinit var observeContinueWatching: ObserveContinueWatchingUseCase

    /**
     * Find anime in the YummyTV catalog by title.
     *
     * @param query A non-blank anime title or part of a title.
     * @return Up to ten matches with catalog IDs, or an empty list if nothing matches.
     * @throws AppFunctionInvalidArgumentException If the title is blank.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun searchAnime(query: String): List<AnimeMatch> = withContext(Dispatchers.IO) {
        val title = query.trim()
        if (title.isEmpty()) {
            throw AppFunctionInvalidArgumentException("Enter an anime title.")
        }
        search(title, limit = SEARCH_RESULT_LIMIT).items.take(SEARCH_RESULT_LIMIT).map { item ->
            AnimeMatch(animeId = item.id, title = item.title)
        }
    }

    /**
     * Get an anime synopsis and release information from the YummyTV catalog.
     *
     * @param animeId A positive YummyTV catalog ID, for example from searchAnime or getAnimeSchedule.
     * @return Public catalog information about the anime.
     * @throws AppFunctionInvalidArgumentException If the ID is zero or negative.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getAnimeDetails(animeId: Int): AnimeDetailsResult = withContext(Dispatchers.IO) {
        requireCatalogId(animeId)
        val details = getDetails(animeId)
        AnimeDetailsResult(
            animeId = details.id,
            title = details.title,
            description = details.description,
            year = details.year,
            status = details.status,
            airedEpisodes = details.episodes?.aired,
            totalEpisodes = details.episodes?.count,
        )
    }

    /**
     * Get the weekly YummyTV anime release schedule.
     *
     * @return Scheduled anime grouped by day, with catalog IDs and available episode release times.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getAnimeSchedule(): List<AnimeScheduleDayResult> = withContext(Dispatchers.IO) {
        getSchedule().map { day ->
            AnimeScheduleDayResult(
                day = day.title,
                entries = day.items.map { item ->
                    AnimeScheduleEntry(
                        animeId = item.animeId,
                        title = item.title,
                        nextEpisodeAt = item.nextDateEpochSeconds,
                        airedEpisodes = item.airedEpisodes,
                        totalEpisodes = item.totalEpisodes,
                    )
                },
            )
        }
    }

    /**
     * Prepare to resume an anime from YummyTV's saved viewing progress.
     *
     * @param animeId The positive YummyTV catalog ID of the anime to resume. Omit it to select the
     * first playable entry in Continue Watching.
     * @return An action the caller can use to open YummyTV and resume the selected anime.
     * @throws AppFunctionInvalidArgumentException If the ID is zero or negative.
     * @throws AppFunctionElementNotFoundException If there is no playable progress for the request.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun continueWatching(animeId: Int? = null): PendingIntent = withContext(Dispatchers.IO) {
        animeId?.let(::requireCatalogId)
        val entry = observeContinueWatching().first().firstOrNull {
            it.animeId > 0 && (animeId == null || it.animeId == animeId) && it.hasPlayableTarget()
        } ?: throw AppFunctionElementNotFoundException("No matching entry in Continue Watching.")

        val intent = Intent(this@BaseYummyTvAppFunctionService, InterfaceRouterActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = "yummytv://continue/${entry.animeId}".toUri()
        }
        PendingIntent.getActivity(this@BaseYummyTvAppFunctionService, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun requireCatalogId(animeId: Int) {
        if (animeId <= 0) {
            throw AppFunctionInvalidArgumentException("The YummyTV catalog ID must be positive.")
        }
    }

    private companion object {
        const val SEARCH_RESULT_LIMIT = 10
    }
}
