package su.afk.yummy.tv.feature.library.thumbnail

import coil3.ImageLoader
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.request.Options
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.utils.episode.episodeGroupKey
import su.afk.yummy.tv.core.utils.kodik.KodikThumbnail
import su.afk.yummy.tv.core.utils.kodik.KodikThumbnailCacheIO
import su.afk.yummy.tv.core.utils.kodik.ResolveKodikThumbnailUrlUseCase
import su.afk.yummy.tv.core.utils.kodik.kodikThumbnailIframeUrl
import su.afk.yummy.tv.domain.anime.usecase.GetCachedAnimeVideosUseCase

/**
 * Резолвит kodik-превью серии из истории просмотров, у которой нет iframe-урла под рукой:
 * ищет серию по [episodeGroupKey] в уже закэшированном (без сети — [GetCachedAnimeVideosUseCase])
 * списке видео аниме, берёт её kodik iframe-урл и уже его отдаёт [ResolveKodikThumbnailUrlUseCase] —
 * дальше как в [su.afk.yummy.tv.core.utils.kodik.KodikThumbnailFetcher], включая тот же
 * [KodikThumbnail.cacheKey] и тот же [KodikThumbnailCacheIO]. Если список видео ещё не закэширован,
 * фетчер ничего не находит и не идёт в сеть сам — экран истории никогда не инициирует
 * `/anime/{id}/videos`.
 */
class HistoryEpisodeThumbnailFetcher(
    private val data: HistoryEpisodeThumbnail,
    private val cacheIO: KodikThumbnailCacheIO,
    private val getCachedAnimeVideos: GetCachedAnimeVideosUseCase,
    private val resolveKodikThumbnailUrl: ResolveKodikThumbnailUrlUseCase,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val videos = runSuspendCatching { getCachedAnimeVideos(data.animeId) }.getOrNull().orEmpty()
        val episodeVideos = videos.filter {
            it.episode.episodeGroupKey() == data.episode.episodeGroupKey()
        }
        val iframeUrl = episodeVideos.kodikThumbnailIframeUrl()
        val cacheKey = iframeUrl?.let { KodikThumbnail(it).cacheKey } ?: data.cacheKey
        return cacheIO.fetch(cacheKey) { iframeUrl?.let { resolveKodikThumbnailUrl(it) } }
    }

    class Factory(
        private val getCachedAnimeVideos: GetCachedAnimeVideosUseCase,
        private val resolveKodikThumbnailUrl: ResolveKodikThumbnailUrlUseCase,
        private val cacheIO: KodikThumbnailCacheIO,
    ) : Fetcher.Factory<HistoryEpisodeThumbnail> {

        override fun create(
            data: HistoryEpisodeThumbnail,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = HistoryEpisodeThumbnailFetcher(
            data = data,
            cacheIO = cacheIO,
            getCachedAnimeVideos = getCachedAnimeVideos,
            resolveKodikThumbnailUrl = resolveKodikThumbnailUrl,
        )
    }
}
