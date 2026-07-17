package su.afk.yummy.tv.android.di

import coil3.ImageLoader
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.request.Options
import su.afk.yummy.tv.core.utils.KodikThumbnail
import su.afk.yummy.tv.core.utils.ResolveKodikThumbnailUrlUseCase

/**
 * Резолвит URL Kodik-превью и передаёт его штатному network fetcher Coil.
 * Картинка читается и сохраняется в disk cache по iframe-ключу.
 */
class KodikThumbnailFetcher(
    private val data: KodikThumbnail,
    private val options: Options,
    private val imageLoader: ImageLoader,
    private val resolveKodikThumbnailUrl: ResolveKodikThumbnailUrlUseCase,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val resolvedUrl = resolveKodikThumbnailUrl(data.iframeUrl)
        val delegatedOptions = options.copy(diskCacheKey = data.cacheKey)
        val mappedData = imageLoader.components.map(
            data = resolvedUrl ?: FALLBACK_URL,
            options = delegatedOptions,
        )
        val delegatedFetcher = checkNotNull(
            imageLoader.components.newFetcher(mappedData, delegatedOptions, imageLoader)?.first
        ) { "No Coil fetcher registered for Kodik thumbnail URL" }
        return delegatedFetcher.fetch()
    }

    class Factory(
        private val resolveKodikThumbnailUrl: ResolveKodikThumbnailUrlUseCase,
    ) : Fetcher.Factory<KodikThumbnail> {

        override fun create(
            data: KodikThumbnail,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = KodikThumbnailFetcher(
            data = data,
            options = options,
            imageLoader = imageLoader,
            resolveKodikThumbnailUrl = resolveKodikThumbnailUrl,
        )
    }

    private companion object {
        const val FALLBACK_URL = "https://offline.invalid/kodik-thumbnail"
    }
}
