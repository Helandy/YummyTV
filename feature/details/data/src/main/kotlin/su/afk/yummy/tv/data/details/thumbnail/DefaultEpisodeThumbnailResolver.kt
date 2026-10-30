package su.afk.yummy.tv.data.details.thumbnail

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.utils.episode.EpisodeThumbnail
import su.afk.yummy.tv.core.utils.episode.EpisodeThumbnailResolver
import su.afk.yummy.tv.core.utils.kodik.ResolveKodikThumbnailUrlUseCase
import su.afk.yummy.tv.core.utils.kodik.kodikThumbnailIframeUrl
import su.afk.yummy.tv.domain.anime.usecase.GetCachedAnimeVideosUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Источники превью серии по порядку: TMDB (кадры от YummyTV API), Tenrai (кадры MyAnimeList), затем Kodik (iframe
 * экрана или записи просмотра или, если его нет, Kodik-видео серии из уже закэшированного списка видео тайтла — сеть
 * за списком не ходит). Kodik остаётся запасным: он нужен, когда ни TMDB, ни Tenrai кадра не знают (свежие серии и
 * тайтлы без соответствия в TMDB). Одна и та же цепочка у эпизодов, «Продолжить», истории и «Отложенных».
 * Готовая картинка с Yani (`imageUrl` записи) в цепочке известной серии не участвует: у неопознанной серии она
 * остаётся единственным вариантом.
 *
 * Источники отдаются потоком кандидатов: загрузчик скачивает первый, а если он не скачался (CDN TMDB у части
 * пользователей заблокирован), берёт следующий; сетевой резолв Kodik выполняется, только когда до него дошла очередь.
 */
@Singleton
class DefaultEpisodeThumbnailResolver @Inject constructor(
    private val tmdbPreviews: TmdbEpisodePreviews,
    private val tenraiThumbnails: TenraiEpisodeThumbnails,
    private val getCachedAnimeVideos: GetCachedAnimeVideosUseCase,
    private val resolveKodikThumbnailUrl: ResolveKodikThumbnailUrlUseCase,
) : EpisodeThumbnailResolver {

    override fun candidateUrls(thumbnail: EpisodeThumbnail): Flow<String> = flow {
        if (thumbnail.isEpisodeKnown) {
            tmdbPreviews.previewUrl(thumbnail.animeId, thumbnail.episode)?.let { emit(it) }
            tenraiThumbnails.previewUrl(thumbnail.animeId, thumbnail.episode)?.let { emit(it) }
        } else {
            // Серия не опознана: опереться больше не на что, готовая картинка экрана — единственный шанс на кадр.
            thumbnail.imageUrl.takeIf { it.isNotBlank() }?.let { emit(it) }
        }
        val iframeUrl = thumbnail.iframeUrl.takeIf { it.isNotBlank() }
            ?: cachedKodikIframeUrl(thumbnail)
            ?: return@flow
        resolveKodikThumbnailUrl(iframeUrl)?.let { emit(it) }
    }

    private suspend fun cachedKodikIframeUrl(thumbnail: EpisodeThumbnail): String? {
        if (!thumbnail.isEpisodeKnown) return null
        val episodeKey = thumbnail.episode.episodeGroupKey()
        return runSuspendCatching { getCachedAnimeVideos(thumbnail.animeId) }
            .getOrNull()
            .orEmpty()
            .filter { it.episode.episodeGroupKey() == episodeKey }
            .kodikThumbnailIframeUrl()
    }
}
