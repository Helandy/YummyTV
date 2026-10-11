package su.afk.yummy.tv.data.videodownload.cache

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.storage.videodownload.VideoDownloadStorage
import javax.inject.Inject

/**
 * Убирает из безлимитного кэша загрузок ([VideoDownloadCacheProvider]) всё, за чем не стоит активная
 * запись: наследие тех версий, где обычное (не офлайн) воспроизведение писало туда же и никогда не
 * вытеснялось, плюс данные уже удалённых серий старой схемы.
 *
 * Проход возможен только когда все активные загрузки перешли на неймспейсинг ключей. Пока жива хоть
 * одна запись схемы [su.afk.yummy.tv.domain.videodownload.model.VideoDownloadCacheKeyScheme.Legacy],
 * её HLS/DASH-сегменты лежат под сырыми URL и неотличимы от чужих — попытка «прибраться» сносила
 * данные всех остальных серий, из-за чего они переставали воспроизводиться.
 */
@OptIn(UnstableApi::class)
class LegacyStreamingCachePruner @Inject constructor(
    private val cacheProvider: VideoDownloadCacheProvider,
    private val store: VideoDownloadStorage,
) {
    suspend fun pruneOrphanedEntries() {
        if (store.hasActiveLegacyCacheKeyDownloads()) return
        val activeCacheKeys = store.getActiveCacheKeys().toSet()
        val activePrefixes = activeCacheKeys.flatMap(::downloadResourcePrefixes)
        withContext(Dispatchers.IO) {
            cacheProvider.cache.keys
                .filterNot { key -> key in activeCacheKeys || activePrefixes.any(key::startsWith) }
                .forEach { key -> runCatching { cacheProvider.cache.removeResource(key) } }
        }
    }
}
