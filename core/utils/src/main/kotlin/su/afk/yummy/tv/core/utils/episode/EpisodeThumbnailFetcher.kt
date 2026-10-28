package su.afk.yummy.tv.core.utils.episode

import coil3.ImageLoader
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.key.Keyer
import coil3.request.Options
import su.afk.yummy.tv.core.utils.kodik.KodikThumbnailCacheIO

/**
 * Читает/пишет кадр серии через [KodikThumbnailCacheIO] (отдельный небольшой disk cache, не общий кэш постеров),
 * а ссылку на него, если на диске его ещё нет, берёт у [EpisodeThumbnailResolver].
 */
class EpisodeThumbnailFetcher(
    private val data: EpisodeThumbnail,
    private val cacheIO: KodikThumbnailCacheIO,
    private val resolver: EpisodeThumbnailResolver,
) : Fetcher {

    override suspend fun fetch(): FetchResult? =
        cacheIO.fetch(
            cacheKey = data.cacheKey,
            fallbackKeys = data.fallbackCacheKeys,
        ) { resolver.resolveUrl(data) }

    class Factory(
        private val resolver: EpisodeThumbnailResolver,
        private val cacheIO: KodikThumbnailCacheIO,
    ) : Fetcher.Factory<EpisodeThumbnail> {

        override fun create(
            data: EpisodeThumbnail,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = EpisodeThumbnailFetcher(data, cacheIO, resolver)
    }
}

/** Даёт Coil стабильный memory-cache key, не зависящий от конечного URL картинки. */
class EpisodeThumbnailKeyer : Keyer<EpisodeThumbnail> {

    override fun key(data: EpisodeThumbnail, options: Options): String = data.cacheKey
}
