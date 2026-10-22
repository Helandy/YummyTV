package su.afk.yummy.tv.android.startup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.aomedia.avif.android.AvifDecoder
import su.afk.yummy.tv.core.model.settings.ImageFormatSupport
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация [ImageFormatSupport]: проверяет, что устройство реально декодирует AVIF и WebP: прогоняет крошечные эталонные картинки
 * из assets через те же декодеры, что использует приложение (libavif для AVIF, [BitmapFactory] для
 * WebP). AVIF считается рабочим, только если декод успешен и уложился в [AVIF_MAX_DECODE_MS] —
 * медленный декодер на слабой приставке хуже, чем WebP.
 */
@Singleton
class ImageFormatProbe @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ImageFormatSupport {

    override suspend fun isAvifSupported(): Boolean =
        withContext(Dispatchers.Default) { probe(AVIF_ASSET, ::decodeAvif) }

    override suspend fun isWebpSupported(): Boolean =
        withContext(Dispatchers.Default) { probe(WEBP_ASSET, ::decodeWebp) }

    // Проба не должна ронять процесс: UnsatisfiedLinkError на чужой ABI, OOM, битый ассет.
    private fun probe(asset: String, decode: (ByteArray) -> Boolean): Boolean =
        try {
            decode(context.assets.open(asset).use { it.readBytes() })
        } catch (_: Exception) {
            false
        } catch (_: LinkageError) {
            false
        } catch (_: OutOfMemoryError) {
            false
        }

    private fun decodeAvif(bytes: ByteArray): Boolean {
        val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
            put(bytes)
            rewind()
        }
        val info = AvifDecoder.Info()
        if (!AvifDecoder.getInfo(buffer, buffer.remaining(), info)) return false
        val bitmap = Bitmap.createBitmap(info.width, info.height, Bitmap.Config.ARGB_8888)
        val startedAt = SystemClock.elapsedRealtime()
        val decoded = AvifDecoder.decode(buffer, buffer.remaining(), bitmap)
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        bitmap.recycle()
        return decoded && elapsed <= AVIF_MAX_DECODE_MS
    }

    private fun decodeWebp(bytes: ByteArray): Boolean {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return false
        bitmap.recycle()
        return true
    }

    private companion object {
        const val AVIF_ASSET = "image_probe/avif_probe.avif"
        const val WEBP_ASSET = "image_probe/webp_probe.webp"
        const val AVIF_MAX_DECODE_MS = 300L
    }
}
