package su.afk.yummy.tv.android.startup

import coil3.network.CacheStrategy
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.Options
import kotlin.time.Duration.Companion.days

/**
 * Статика нашего сервера (`/static/`, например декор аватарки) отдаётся без Cache-Control и ETag,
 * поэтому стандартная стратегия Coil перекачивает её при каждом открытии; у картинок CDN (постеры, бэкдропы)
 * срок задаёт чужой Cache-Control, часто короткий. Здесь успешный ответ из кэша — статика или любая
 * картинка (Content-Type image) — считаем свежим [StaticFreshness]; всё остальное решает [CacheStrategy.DEFAULT]. Адрес статики
 * берётся из [staticPrefix] при каждом запросе: хост API можно заменить удалённо.
 */
internal class StaticAssetCacheStrategy(
    private val staticPrefix: () -> String,
    private val delegate: CacheStrategy = CacheStrategy.DEFAULT,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : CacheStrategy {

    override suspend fun read(
        cacheResponse: NetworkResponse,
        networkRequest: NetworkRequest,
        options: Options,
    ): CacheStrategy.ReadResult {
        val age = nowMillis() - cacheResponse.responseMillis
        val isFresh = cacheResponse.code == 200 && age in 0..StaticFreshness.inWholeMilliseconds
        val isStableAsset = isOwnStatic(networkRequest.url, staticPrefix()) || isImage(cacheResponse.headers["Content-Type"])
        return if (isFresh && isStableAsset) {
            CacheStrategy.ReadResult(cacheResponse)
        } else {
            delegate.read(cacheResponse, networkRequest, options)
        }
    }

    override suspend fun write(
        cacheResponse: NetworkResponse?,
        networkRequest: NetworkRequest,
        networkResponse: NetworkResponse,
        options: Options,
    ): CacheStrategy.WriteResult = delegate.write(cacheResponse, networkRequest, networkResponse, options)

    internal companion object {
        val StaticFreshness = 30.days

        fun isOwnStatic(url: String, staticPrefix: String): Boolean = staticPrefix.isNotEmpty() && url.startsWith(staticPrefix)

        fun isImage(contentType: String?): Boolean = contentType?.trimStart()?.startsWith("image/", ignoreCase = true) == true
    }
}
