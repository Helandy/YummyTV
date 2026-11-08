package su.afk.yummy.tv.feature.player.common.service

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.session.CacheBitmapLoader
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

/**
 * Загрузчик обложек для медиа-сессии, который отдаёт каждому потребителю собственную копию битмапа.
 *
 * Стандартный загрузчик media3 кэширует последний битмап и раздаёт один и тот же экземпляр и
 * метаданным сессии, и уведомлению. Если кто-то из них успевает его освободить, системный
 * `MediaSession.setMetadata` падает с «cannot use a recycled source in createBitmap». Копия
 * изолирует потребителей друг от друга, а если исходник уже освобождён, будущее завершается
 * ошибкой, которую media3 просто логирует, и обложка пропускается без краша.
 */
@OptIn(UnstableApi::class)
internal class CopyingBitmapLoader(context: Context) : BitmapLoader {
    private val delegate: BitmapLoader =
        CacheBitmapLoader(DataSourceBitmapLoader.Builder(context).build())

    override fun supportsMimeType(mimeType: String): Boolean = delegate.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        delegate.decodeBitmap(data).copied()

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        delegate.loadBitmap(uri).copied()

    private fun ListenableFuture<Bitmap>.copied(): ListenableFuture<Bitmap> =
        Futures.transform(
            this,
            { source -> checkNotNull(source).copy(source.config ?: Bitmap.Config.ARGB_8888, false) },
            MoreExecutors.directExecutor(),
        )
}
