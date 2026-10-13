package su.afk.yummy.tv.android.startup

import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.Size
import coil3.size.pxOrElse
import kotlinx.coroutines.runInterruptible
import okio.ByteString.Companion.encodeUtf8
import org.aomedia.avif.android.AvifDecoder
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * Программный AVIF-декодер на libavif + dav1d.
 *
 * Системный декодер на части ТВ-приставок уходит в аппаратный AV1-кодек: ~3 с на картинку, сбои при
 * параллельных запросах, а кадры 1919x1079 не декодируются вовсе. Здесь декодирование идёт в CPU и
 * сразу уменьшается до размера таргета, поэтому в memory cache попадает уменьшенный битмап.
 */
internal class AvifImageDecoder(
    private val source: SourceFetchResult,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult = runInterruptible {
        val bytes = source.source.source().readByteArray()
        val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
            put(bytes)
            rewind()
        }

        val info = AvifDecoder.Info()
        if (!AvifDecoder.getInfo(buffer, buffer.remaining(), info)) {
            throw IOException("Не удалось прочитать заголовок AVIF")
        }

        val full = Bitmap.createBitmap(info.width, info.height, Bitmap.Config.ARGB_8888)
        if (!AvifDecoder.decode(buffer, buffer.remaining(), full)) {
            full.recycle()
            throw IOException("Не удалось декодировать AVIF")
        }

        val multiplier = options.size.targetMultiplier(info.width, info.height, options)
        if (multiplier >= 1.0) {
            return@runInterruptible DecodeResult(image = full.asImage(), isSampled = false)
        }

        val scaled = Bitmap.createScaledBitmap(
            full,
            (info.width * multiplier).roundToInt().coerceAtLeast(1),
            (info.height * multiplier).roundToInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== full) full.recycle()
        DecodeResult(image = scaled.asImage(), isSampled = true)
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            val isAvif = result.mimeType == AVIF_MIME_TYPE ||
                result.source.source().run { rangeEquals(4, BRAND_AVIF) || rangeEquals(4, BRAND_AVIS) }
            return if (isAvif) AvifImageDecoder(result, options) else null
        }

        private companion object {
            const val AVIF_MIME_TYPE = "image/avif"
            val BRAND_AVIF = "ftypavif".encodeUtf8()
            val BRAND_AVIS = "ftypavis".encodeUtf8()
        }
    }
}

private fun Size.targetMultiplier(srcWidth: Int, srcHeight: Int, options: Options): Double {
    if (this == Size.ORIGINAL) return 1.0
    val dstWidth = width.pxOrElse { srcWidth }
    val dstHeight = height.pxOrElse { srcHeight }
    return DecodeUtils.computeSizeMultiplier(
        srcWidth = srcWidth,
        srcHeight = srcHeight,
        dstWidth = dstWidth,
        dstHeight = dstHeight,
        scale = options.scale,
    )
}
