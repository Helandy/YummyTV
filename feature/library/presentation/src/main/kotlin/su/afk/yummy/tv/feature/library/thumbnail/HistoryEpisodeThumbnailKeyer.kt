package su.afk.yummy.tv.feature.library.thumbnail

import coil3.key.Keyer
import coil3.request.Options
import su.afk.yummy.tv.feature.library.model.HistoryEpisodeThumbnail

/** Даёт Coil стабильный memory-cache key, не зависящий от конечного URL картинки. */
class HistoryEpisodeThumbnailKeyer : Keyer<HistoryEpisodeThumbnail> {

    override fun key(data: HistoryEpisodeThumbnail, options: Options): String = data.cacheKey
}
