package su.afk.yummy.tv.feature.library.thumbnail

import coil3.ImageLoader
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.request.Options
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.utils.kodik.KodikThumbnail
import su.afk.yummy.tv.core.utils.kodik.KodikThumbnailCacheIO
import su.afk.yummy.tv.core.utils.kodik.ResolveKodikThumbnailUrlUseCase
import su.afk.yummy.tv.core.utils.kodik.kodikThumbnailIframeUrl
import su.afk.yummy.tv.domain.anime.usecase.GetCachedAnimeVideosUseCase
import su.afk.yummy.tv.feature.library.model.HistoryEpisodeThumbnail

/**
 * Резолвит kodik-превью серии из истории просмотров, у которой нет iframe-урла под рукой:
 * ищет серию по [episodeGroupKey] в уже закэшированном (без сети — [GetCachedAnimeVideosUseCase])
 * списке видео аниме, берёт её kodik iframe-урл и уже его отдаёт [ResolveKodikThumbnailUrlUseCase] —
 * дальше как в [su.afk.yummy.tv.core.utils.kodik.KodikThumbnailFetcher], включая тот же
 * [KodikThumbnail.cacheKey] и тот же [KodikThumbnailCacheIO]. Превью серии, уже лежащее на диске,
 * отдаётся сразу, без списка видео. Если список видео ещё не закэширован,
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
        cacheIO.fetchTenrai(data.animeId, data.episode)?.let { return it }
        cacheIO.fetch(data.cacheKey) { null }?.let { return it }
        val videos = runSuspendCatching { getCachedAnimeVideos(data.animeId) }.getOrNull().orEmpty()
        val episodeVideos = videos.filter {
            it.episode.episodeGroupKey() == data.episode.episodeGroupKey()
        }
        val iframeUrl = episodeVideos.kodikThumbnailIframeUrl() ?: return null
        return cacheIO.fetch(
            cacheKey = data.cacheKey,
            fallbackKeys = listOf(KodikThumbnail(iframeUrl).legacyCacheKey),
        ) { resolveKodikThumbnailUrl(iframeUrl) }
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
