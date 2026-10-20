package su.afk.yummy.tv.feature.details.details.handler

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import su.afk.yummy.tv.domain.account.usecase.GetAnimeListStatsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAnimeRatingSummaryUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeTrailersUseCase
import su.afk.yummy.tv.domain.bloggers.usecase.GetAnimeBloggerVideosUseCase
import su.afk.yummy.tv.feature.details.details.model.DetailsRatingOverview
import su.afk.yummy.tv.feature.details.mapper.detailsRatingOverview
import javax.inject.Inject

/** Грузит второстепенные блоки деталей: трейлеры, наличие видео блогеров, сводку рейтинга и статистику списков. */
internal class DetailsExtrasHandler @Inject constructor(
    private val getAnimeTrailers: GetAnimeTrailersUseCase,
    private val getAnimeBloggerVideos: GetAnimeBloggerVideosUseCase,
    private val getAnimeRatingSummary: GetAnimeRatingSummaryUseCase,
    private val getAnimeListStats: GetAnimeListStatsUseCase,
) {
    suspend fun load(animeId: Int): DetailsExtrasResult = coroutineScope {
        val trailers = async { runSuspendCatching { getAnimeTrailers(animeId) } }
        val bloggerVideos = async { runSuspendCatching { getAnimeBloggerVideos(animeId, limit = 1) } }
        val overview = async { loadRatingOverview(animeId) }
        DetailsExtrasResult(
            trailers = trailers.await().getOrDefault(emptyList()),
            // Сбой запроса не прячет карточку: скрываем только когда видео точно нет.
            hasBloggerVideos = bloggerVideos.await().getOrNull()?.isNotEmpty() ?: true,
            ratingOverview = overview.await(),
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
    val hasBloggerVideos: Boolean = true,
)
