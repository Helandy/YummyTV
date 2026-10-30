package su.afk.yummy.tv.android.startup

import coil3.network.CacheStrategy
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.Options
import kotlin.time.Duration.Companion.days

/**
 * Статика нашего сервера (`/static/`, например декор аватарки) отдаётся без Cache-Control и ETag,
 * поэтому стандартная стратегия Coil перекачивает её при каждом открытии. Здесь успешный ответ
 * из кэша считаем свежим [StaticFreshness]; всё остальное решает [CacheStrategy.DEFAULT].
 */
internal class StaticAssetCacheStrategy(
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
        return if (isFresh && isOwnStatic(networkRequest.url)) {
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
        val StaticFreshness = 7.days
        private const val StaticPrefix = "https://yummytv.afk.su/static/"

        fun isOwnStatic(url: String): Boolean = url.startsWith(StaticPrefix)
    }
}
