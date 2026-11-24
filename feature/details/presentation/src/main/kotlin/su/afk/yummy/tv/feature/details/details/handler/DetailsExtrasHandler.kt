package su.afk.yummy.tv.feature.details.details.handler

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.model.anime.AnimeEpisodeFrames
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import su.afk.yummy.tv.domain.account.usecase.GetAnimeCollectionsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeListStatsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeRatingSummaryUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeEpisodeFramesUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeTrailersUseCase
import su.afk.yummy.tv.feature.details.details.model.DetailsCollections
import su.afk.yummy.tv.feature.details.details.model.DetailsRatingOverview
import su.afk.yummy.tv.feature.details.mapper.detailsRatingOverview
import su.afk.yummy.tv.feature.details.utils.collectionsBadge
import javax.inject.Inject

/**
 * Грузит второстепенные блоки деталей: трейлеры, сводку рейтинга, статистику списков, число коллекций и кадры серий
 * (кадры серий для ряда скриншотов). Каждый блок грузится отдельным методом, чтобы ViewModel запускала их параллельно.
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
    suspend fun loadTrailers(animeId: Int, trailersCount: Int?): List<AnimeTrailer> =
        if (trailersCount == 0) {
            emptyList()
        } else {
            runSuspendCatching { getAnimeTrailers(animeId) }.getOrDefault(emptyList())
        }

    suspend fun loadCollections(animeId: Int): DetailsCollections {
        val loaded = runSuspendCatching { getAnimeCollections(animeId, limit = COLLECTIONS_PAGE_LIMIT) }.getOrNull()
        return DetailsCollections(
            badge = loaded?.let { collectionsBadge(it.size, COLLECTIONS_PAGE_LIMIT) },
            items = loaded.orEmpty().distinctBy { it.id },
        )
    }

    suspend fun loadEpisodeFrames(animeId: Int): AnimeEpisodeFrames =
        runSuspendCatching { getAnimeEpisodeFrames(animeId) }.getOrDefault(AnimeEpisodeFrames())

    suspend fun refreshTrailers(animeId: Int, trailersCount: Int?): Result<List<AnimeTrailer>> =
        runSuspendCatching {
            if (trailersCount == 0) emptyList() else getAnimeTrailers(animeId, forceRefresh = true)
        }

    suspend fun refreshCollections(animeId: Int): Result<DetailsCollections> = runSuspendCatching {
        val items = getAnimeCollections(animeId, limit = COLLECTIONS_PAGE_LIMIT, forceRefresh = true)
        DetailsCollections(badge = collectionsBadge(items.size, COLLECTIONS_PAGE_LIMIT), items = items.distinctBy { it.id })
    }

    suspend fun refreshEpisodeFrames(animeId: Int): Result<AnimeEpisodeFrames> =
        runSuspendCatching { getAnimeEpisodeFrames(animeId, forceRefresh = true) }

    suspend fun refreshRatingOverview(animeId: Int): Result<DetailsRatingOverview?> = runSuspendCatching {
        coroutineScope {
            val summary = async { getAnimeRatingSummary(animeId, forceRefresh = true) }
            val stats = async { getAnimeListStats(animeId, forceRefresh = true) }
            detailsRatingOverview(summary.await(), stats.await())
        }
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

private const val COLLECTIONS_PAGE_LIMIT = 20
