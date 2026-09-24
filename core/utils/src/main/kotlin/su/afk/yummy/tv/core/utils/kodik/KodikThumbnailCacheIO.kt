package su.afk.yummy.tv.core.utils.kodik

import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.SourceFetchResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okio.Buffer
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.utils.kodik.di.KodikHttpClient
import su.afk.yummy.tv.core.utils.kodik.di.KodikThumbnailDiskCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Читает/пишет байты Kodik-превью серии напрямую в выделенный [DiskCache] (15 МБ, отдельно от
 * общего кэша картинок Coil). Используется и [KodikThumbnailFetcher] (продолжить просмотр, серии),
 * и History-фетчером из feature:library — по одному и тому же ключу на iframe-урл, поэтому одна и
 * та же серия хранится на диске один раз, независимо от того, с какого экрана она была впервые
 * открыта.
 */
@Singleton
class KodikThumbnailCacheIO @Inject constructor(
    @KodikThumbnailDiskCache private val diskCache: DiskCache,
    @KodikHttpClient private val httpClient: HttpClient,
) {

    suspend fun fetch(cacheKey: String, resolvedUrl: String?): FetchResult? {
        readSnapshot(cacheKey)?.let { return it }
        val url = resolvedUrl ?: return null
        return downloadAndCache(cacheKey, url)
    }

    private fun readSnapshot(cacheKey: String): SourceFetchResult? {
        val snapshot = diskCache.openSnapshot(cacheKey) ?: return null
        return SourceFetchResult(
            source = ImageSource(snapshot.data, diskCache.fileSystem, cacheKey, snapshot),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    private suspend fun downloadAndCache(cacheKey: String, url: String): SourceFetchResult? {
        val response = runSuspendCatching { httpClient.get(url) }.getOrElse { return null }
        if (!response.status.isSuccess()) return null
        val bytes = response.bodyAsBytes()
        val mimeType = response.contentType()?.toString()

        val editor = diskCache.openEditor(cacheKey)
            ?: return SourceFetchResult(
                source = ImageSource(Buffer().apply { write(bytes) }, diskCache.fileSystem),
                mimeType = mimeType,
                dataSource = DataSource.NETWORK,
            )

        return try {
            diskCache.fileSystem.write(editor.data) { write(bytes) }
            val snapshot = editor.commitAndOpenSnapshot() ?: return null
            SourceFetchResult(
                source = ImageSource(snapshot.data, diskCache.fileSystem, cacheKey, snapshot),
                mimeType = mimeType,
                dataSource = DataSource.NETWORK,
            )
        } catch (_: Throwable) {
            editor.abort()
            currentCoroutineContext().ensureActive()
            null
        }
    }
}
