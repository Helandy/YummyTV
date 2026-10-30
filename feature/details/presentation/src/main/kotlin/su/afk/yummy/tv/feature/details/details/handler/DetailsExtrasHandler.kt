package su.afk.yummy.tv.feature.details.details.handler

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.model.anime.AnimeEpisodeFrames
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.usecase.GetAnimeCollectionsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeListStatsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeRatingSummaryUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeEpisodeFramesUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeTrailersUseCase
import su.afk.yummy.tv.feature.details.details.model.DetailsRatingOverview
import su.afk.yummy.tv.feature.details.mapper.detailsRatingOverview
import su.afk.yummy.tv.feature.details.utils.collectionsBadge
import javax.inject.Inject

/**
 * Грузит второстепенные блоки деталей: трейлеры, сводку рейтинга, статистику списков, число коллекций и кадры серий
 * (кадры серий для ряда скриншотов).
 * Счётчики трейлеров и видео блогеров приходят в самих деталях, отдельных запросов за ними нет.
 */
internal class DetailsExtrasHandler @Inject constructor(
    private val getAnimeTrailers: GetAnimeTrailersUseCase,
    private val getAnimeRatingSummary: GetAnimeRatingSummaryUseCase,
    private val getAnimeListStats: GetAnimeListStatsUseCase,
    private val getAnimeCollections: GetAnimeCollectionsUseCase,
    private val getAnimeEpisodeFrames: GetAnimeEpisodeFramesUseCase,
) {
    /** [trailersCount] — счётчик из деталей: при нуле запрос трейлеров пропускается, `null` — счётчик неизвестен. */
    suspend fun load(animeId: Int, trailersCount: Int?): DetailsExtrasResult = coroutineScope {
        val trailers = async {
            if (trailersCount == 0) Result.success(emptyList()) else runSuspendCatching { getAnimeTrailers(animeId) }
        }
        val overview = async { loadRatingOverview(animeId) }
        val collections = async { runSuspendCatching { getAnimeCollections(animeId, limit = COLLECTIONS_PAGE_LIMIT) } }
        val episodeFrames = async { runSuspendCatching { getAnimeEpisodeFrames(animeId) } }
        val loadedCollections = collections.await().getOrNull()
        DetailsExtrasResult(
            trailers = trailers.await().getOrDefault(emptyList()),
            ratingOverview = overview.await(),
            collectionsBadge = loadedCollections?.let { collectionsBadge(it.size, COLLECTIONS_PAGE_LIMIT) },
            collections = loadedCollections.orEmpty().distinctBy { it.id },
            episodeFrames = episodeFrames.await().getOrDefault(AnimeEpisodeFrames()),
        )
    }

    /** Ошибки блоков тихие: что не загрузилось, то просто не показывается. */
    suspend fun loadRatingOverview(animeId: Int): DetailsRatingOverview? = coroutineScope {
        val summary = async { runSuspendCatching { getAnimeRatingSummary(animeId) } }
        val listStats = async { runSuspendCatching { getAnimeListStats(animeId) } }
        detailsRatingOverview(
            summary = summary.await().getOrNull(),
            listStats = listStats.await().getOrNull(),
        )
    }
}

/** Результат загрузки второстепенных блоков деталей. */
internal data class DetailsExtrasResult(
    val trailers: List<AnimeTrailer>,
    val ratingOverview: DetailsRatingOverview?,
    /** Бэйдж числа коллекций; `null`, если их нет или список не загрузился. */
    val collectionsBadge: String? = null,
    /** Коллекции с тайтлом из того же запроса, что и бэйдж, — для ленты в деталях. */
    val collections: List<AnimeCollectionSummary> = emptyList(),
    /** Кадры серий из TMDB и Tenrai для ряда скриншотов; пустая модель, если их нет или запрос не удался. */
    val episodeFrames: AnimeEpisodeFrames = AnimeEpisodeFrames(),
)

private const val COLLECTIONS_PAGE_LIMIT = 20
