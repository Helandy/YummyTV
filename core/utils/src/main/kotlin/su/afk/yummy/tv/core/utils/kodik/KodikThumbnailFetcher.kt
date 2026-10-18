package su.afk.yummy.tv.core.utils.kodik

import coil3.ImageLoader
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.request.Options

/**
 * Резолвит URL Kodik-превью и читает/пишет картинку через [KodikThumbnailCacheIO] —
 * отдельный, небольшой disk cache (не общий кэш постеров). Ключ — [KodikThumbnail.cacheKey]
 * (общий для серии), при промахе — прежний ключ по iframe-урлу, и только потом сеть.
 */
class KodikThumbnailFetcher(
    private val data: KodikThumbnail,
    private val cacheIO: KodikThumbnailCacheIO,
    private val resolveKodikThumbnailUrl: ResolveKodikThumbnailUrlUseCase,
) : Fetcher {

    override suspend fun fetch(): FetchResult? =
        cacheIO.fetch(
            cacheKey = data.cacheKey,
            fallbackKeys = listOf(data.legacyCacheKey).filter { it != data.cacheKey },
        ) { resolveKodikThumbnailUrl(data.iframeUrl) }

    class Factory(
        private val resolveKodikThumbnailUrl: ResolveKodikThumbnailUrlUseCase,
        private val cacheIO: KodikThumbnailCacheIO,
    ) : Fetcher.Factory<KodikThumbnail> {

        override fun create(
            data: KodikThumbnail,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = KodikThumbnailFetcher(
            data = data,
            cacheIO = cacheIO,
            resolveKodikThumbnailUrl = resolveKodikThumbnailUrl,
        )
    }
}
