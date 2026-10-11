package su.afk.yummy.tv.data.videodownload.engine

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.ParserException
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import androidx.media3.extractor.mp4.Mp4Extractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import su.afk.yummy.tv.data.videodownload.cache.VideoDownloadCacheProvider
import su.afk.yummy.tv.data.videodownload.cache.downloadCacheKeyFactoryFor
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import java.io.EOFException
import java.io.IOException
import javax.inject.Inject

/**
 * Проверяет скачанный прогрессивный mp4 прямо в кэше загрузок, не обращаясь к сети.
 *
 * Прогоняет файл через тот же `Mp4Extractor`, что и воспроизведение: он читает таблицу сэмплов и
 * длины NAL-блоков и бросает `Invalid NAL length` на порче именно того вида, на который падает
 * плеер. Данные сэмплов отбрасываются, поэтому проверка стоит одного последовательного чтения
 * файла с диска.
 *
 * Ограничение: порча полезной нагрузки внутри NAL-блока при целых длинах не видна ни экстрактору,
 * ни здесь. CDN не отдаёт ни хэша, ни ETag, сверить файл с источником без повторного скачивания
 * нельзя.
 */
@OptIn(UnstableApi::class)
internal class DownloadIntegrityVerifier @Inject constructor(
    private val cacheProvider: VideoDownloadCacheProvider,
) {

    /** Итог проверки скачанного файла. */
    sealed interface Result {
        /** Контейнер разобран до конца без ошибок. */
        data object Valid : Result

        /** Проверить не удалось (не mp4, нет данных в кэше): загрузку это не отменяет. */
        data class Skipped(val reason: String) : Result

        /** Файл повреждён; [position] — смещение, на котором экстрактор остановился. */
        data class Corrupt(val position: Long, val detail: String) : Result
    }

    suspend fun verify(item: VideoDownloadItem): Result =
        runInterruptible(Dispatchers.IO) { scan(item) }

    /**
     * Выбрасывает из кэша закэшированные куски вокруг [position] (по одному в обе стороны от
     * содержащего её), чтобы повторная загрузка перекачала только их. `null` — весь файл целиком.
     *
     * @return сколько байт выброшено.
     */
    suspend fun evict(item: VideoDownloadItem, position: Long?): Long =
        runInterruptible(Dispatchers.IO) {
            val cache = cacheProvider.cache
            if (position == null) {
                val before = cache.cacheSpace
                cache.removeResource(item.cacheKey)
                return@runInterruptible (before - cache.cacheSpace).coerceAtLeast(0L)
            }
            val spans = cache.getCachedSpans(item.cacheKey).sortedBy { it.position }
            if (spans.isEmpty()) return@runInterruptible 0L
            val index = spans.indexOfLast { it.position <= position }.coerceAtLeast(0)
            val victims = spans.subList(
                (index - 1).coerceAtLeast(0),
                (index + 2).coerceAtMost(spans.size),
            )
            victims.sumOf { span ->
                cache.removeSpan(span)
                span.length
            }
        }

    private fun scan(item: VideoDownloadItem): Result {
        val dataSource = createDataSource(item)
        try {
            return scanMp4 { position -> open(dataSource, item, position) }
        } finally {
            runCatching { dataSource.close() }
        }
    }

    /** Только чтение из кэша: промах отдаётся ошибкой, а не уходит в сеть. */
    private fun createDataSource(item: VideoDownloadItem): DataSource =
        CacheDataSource.Factory()
            .setCache(cacheProvider.cache)
            .setUpstreamDataSourceFactory { CacheMissDataSource() }
            .apply {
                downloadCacheKeyFactoryFor(
                    scheme = item.cacheKeyScheme,
                    downloadCacheKey = item.cacheKey,
                    manifestUri = item.streamUrl,
                    legacyRotating = false,
                )?.let(::setCacheKeyFactory)
            }
            .createDataSource()

    private fun open(
        dataSource: DataSource,
        item: VideoDownloadItem,
        position: Long,
    ): DefaultExtractorInput {
        runCatching { dataSource.close() }
        val spec = DataSpec.Builder()
            .setUri(item.streamUrl)
            .setKey(item.cacheKey)
            .setPosition(position)
            .build()
        var length = dataSource.open(spec)
        if (length != C.LENGTH_UNSET.toLong()) length += position
        return DefaultExtractorInput(dataSource, position, length)
    }

    /** Заглушка вместо сети: любое обращение к недостающим байтам — ошибка, а не скачивание. */
    private class CacheMissDataSource : DataSource {
        override fun addTransferListener(transferListener: TransferListener) = Unit

        override fun open(dataSpec: DataSpec): Long =
            throw IOException("Not in cache: position=${dataSpec.position}")

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            throw IOException("Not in cache")

        override fun getUri(): Uri? = null

        override fun close() = Unit
    }

    private object DiscardingExtractorOutput : ExtractorOutput {
        override fun track(id: Int, type: Int): TrackOutput = DiscardingTrackOutput()
        override fun endTracks() = Unit
        override fun seekMap(seekMap: SeekMap) = Unit
    }

    /** Читает данные сэмпла с диска и выбрасывает: нужна лишь валидация разметки. */
    private class DiscardingTrackOutput : TrackOutput {
        private val scratch = ByteArray(SCRATCH_BYTES)

        override fun format(format: androidx.media3.common.Format) = Unit

        override fun sampleData(
            input: DataReader,
            length: Int,
            allowEndOfInput: Boolean,
            sampleDataPart: Int,
        ): Int {
            val read = input.read(scratch, 0, minOf(length, scratch.size))
            if (read == C.RESULT_END_OF_INPUT && !allowEndOfInput) throw EOFException()
            return read
        }

        override fun sampleData(data: ParsableByteArray, length: Int, sampleDataPart: Int) {
            data.skipBytes(length)
        }

        override fun sampleMetadata(
            timeUs: Long,
            flags: Int,
            size: Int,
            offset: Int,
            cryptoData: TrackOutput.CryptoData?,
        ) = Unit
    }

    internal companion object {
        private const val SCRATCH_BYTES = 64 * 1024

        /**
         * Разбирает mp4 до конца; [openInput] открывает вход с заданного смещения (экстрактор
         * просит перемотку, например к `moov` в конце файла).
         */
        fun scanMp4(openInput: (position: Long) -> DefaultExtractorInput): Result {
            val extractor = Mp4Extractor()
            // Последний открытый вход: по его позиции видно, где остановился разбор.
            var lastInput: DefaultExtractorInput? = null
            try {
                var input = openInput(0L).also { lastInput = it }
                if (!extractor.sniff(input)) return Result.Skipped("not an mp4 container")
                extractor.init(DiscardingExtractorOutput)
                val holder = PositionHolder()
                while (true) {
                    if (Thread.interrupted()) throw InterruptedException()
                    when (extractor.read(input, holder)) {
                        Extractor.RESULT_CONTINUE -> Unit
                        Extractor.RESULT_SEEK -> {
                            input = openInput(holder.position).also { lastInput = it }
                        }

                        Extractor.RESULT_END_OF_INPUT -> return Result.Valid
                    }
                }
            } catch (error: ParserException) {
                return if (error.contentIsMalformed) {
                    Result.Corrupt(lastInput?.position ?: 0L, error.message.orEmpty())
                } else {
                    Result.Skipped("unsupported container: ${error.message}")
                }
            } catch (error: EOFException) {
                // Заявленный сэмпл вылезает за конец данных: файл усечён.
                return Result.Corrupt(lastInput?.position ?: 0L, "truncated: ${error.message}")
            } catch (error: IOException) {
                return Result.Skipped("cache read failed: ${error.message}")
            } finally {
                extractor.release()
            }
        }
    }
}
