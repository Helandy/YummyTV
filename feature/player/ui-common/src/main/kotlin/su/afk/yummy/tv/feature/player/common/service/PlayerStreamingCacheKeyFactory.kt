package su.afk.yummy.tv.feature.player.common.service

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheKeyFactory
import su.afk.yummy.tv.core.utils.player.isOkCdnHost
import su.afk.yummy.tv.feature.player.common.utils.okCdnStableCacheKey

/**
 * Ключи онлайн-кэша воспроизведения.
 *
 * Подписанные ссылки okcdn нормализуются до идентичности файла
 * ([okCdnStableCacheKey]) — иначе перерезолв или переезд на резервный узел меняет URI, и уже
 * скачанный буфер приходится качать заново. Всё остальное обслуживает ключ по умолчанию: срезать
 * подпись у произвольного CDN нельзя, можно склеить разные дорожки одного манифеста.
 */
@OptIn(UnstableApi::class)
internal class PlayerStreamingCacheKeyFactory : CacheKeyFactory {

    override fun buildCacheKey(dataSpec: DataSpec): String =
        dataSpec.key
            ?: dataSpec.uri
                .takeIf { it.host?.isOkCdnHost() == true }
                ?.let(::okCdnStableCacheKey)
            ?: CacheKeyFactory.DEFAULT.buildCacheKey(dataSpec)
}
