package su.afk.yummy.tv.core.utils.kodik

import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.SourceFetchResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import okio.Buffer
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.utils.kodik.di.KodikHttpClient
import su.afk.yummy.tv.core.utils.kodik.di.KodikThumbnailDiskCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Читает/пишет байты Kodik-превью серии напрямую в выделенный [DiskCache] (15 МБ, отдельно от
 * общего кэша картинок Coil). Используется [su.afk.yummy.tv.core.utils.episode.EpisodeThumbnailFetcher] по ключу серии
 * ([su.afk.yummy.tv.core.utils.episode.episodeThumbnailCacheKey]), поэтому одна и та же серия хранится на диске один раз, независимо
 * от того, с какого экрана и какой озвучки она была впервые открыта.
 */
@Singleton
class KodikThumbnailCacheIO @Inject constructor(
    @KodikThumbnailDiskCache private val diskCache: DiskCache,
    @KodikHttpClient private val httpClient: HttpClient,
) {

    /**
     * Сначала отдаёт снапшот с диска по [cacheKey]; [candidates] (ссылки по приоритету, часть из них резолвится по сети,
     * например через HTML iframe Kodik) трогается только при промахе — иначе после рестарта процесса каждое превью
     * ждало бы сеть. Ссылки пробуются по очереди, пока одна не скачается (у части пользователей CDN первого источника
     * недоступен). Скачанное пишется под [cacheKey].
     */
    suspend fun fetch(cacheKey: String, candidates: Flow<String>): FetchResult? {
        readSnapshot(cacheKey)?.let { return it }
        return candidates.mapNotNull { url -> downloadAndCache(cacheKey, url) }.firstOrNull()
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
        val contentType = response.contentType()
        // Заглушка/капча с кодом 200 иначе осела бы в кэше навсегда: снапшот отдаётся без сети.
        if (contentType != null && !contentType.match(ContentType.Image.Any)) return null
        val bytes = runSuspendCatching { response.bodyAsBytes() }.getOrElse { return null }
        if (bytes.isEmpty()) return null
        val mimeType = contentType?.toString()

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
