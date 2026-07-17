package su.afk.yummy.tv.android.di

import coil3.key.Keyer
import coil3.request.Options
import su.afk.yummy.tv.core.utils.KodikThumbnail

/** Даёт Coil стабильный memory-cache key, не зависящий от конечного URL картинки. */
class KodikThumbnailKeyer : Keyer<KodikThumbnail> {

    override fun key(data: KodikThumbnail, options: Options): String = data.cacheKey
}
