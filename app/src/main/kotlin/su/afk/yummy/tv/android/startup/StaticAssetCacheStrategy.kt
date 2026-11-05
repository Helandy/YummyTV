package su.afk.yummy.tv.android.startup

import coil3.network.CacheStrategy
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import coil3.request.Options
import okio.BufferedSink
import okio.FileSystem
import okio.IOException
import okio.Path
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
    ): CacheStrategy.WriteResult {
        val result = delegate.write(cacheResponse, networkRequest, networkResponse, options)
        val response = result.response ?: return result
        val body = response.body ?: return result
        val expected = expectedBodyLength(response.headers) ?: return result
        return CacheStrategy.WriteResult(response.copy(body = LengthCheckedBody(body, expected)))
    }

    internal companion object {
        val StaticFreshness = 30.days

        fun isOwnStatic(url: String, staticPrefix: String): Boolean = staticPrefix.isNotEmpty() && url.startsWith(staticPrefix)

        /** Длина тела из `Content-Length`; `null`, если заголовка нет или тело сжато (длина — сжатая). */
        fun expectedBodyLength(headers: NetworkHeaders): Long? {
            val encoding = headers["Content-Encoding"]
            if (encoding != null && !encoding.equals("identity", ignoreCase = true)) return null
            return headers["Content-Length"]?.trim()?.toLongOrNull()?.takeIf { it >= 0 }
        }

        fun isImage(contentType: String?): Boolean = contentType?.trimStart()?.startsWith("image/", ignoreCase = true) == true
    }
}

/**
 * Тело ответа, которое не даёт записать в дисковый кэш оборванную картинку: если после записи размер
 * не равен `Content-Length`, бросается [IOException] — Coil откатывает запись, запрос завершается ошибкой
 * (срабатывает `onError` и цепочка запасных ссылок), а при повторе картинка качается заново.
 * Иначе обрезанный JPEG попадал в кэш и 30 дней отдавался как «свежий» (на экране — половина постера).
 */
private class LengthCheckedBody(
    private val delegate: NetworkResponseBody,
    private val expectedLength: Long,
) : NetworkResponseBody {

    override suspend fun writeTo(sink: BufferedSink) {
        val before = sink.buffer.size
        delegate.writeTo(sink)
        sink.flush()
        // Размер приёмника известен только для буфера; для остальных приёмников проверка невозможна.
        val written = sink.buffer.size - before
        if (written in 0 until expectedLength) throw truncated(written)
    }

    override suspend fun writeTo(fileSystem: FileSystem, path: Path) {
        delegate.writeTo(fileSystem, path)
        val written = fileSystem.metadata(path).size ?: return
        if (written != expectedLength) {
            fileSystem.delete(path, mustExist = false)
            throw truncated(written)
        }
    }

    override fun close() = delegate.close()

    private fun truncated(written: Long) = IOException("Картинка оборвана: получено $written из $expectedLength байт")
}
