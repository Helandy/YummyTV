package su.afk.yummy.tv.feature.account.profileedit.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.domain.account.model.ProfileImageKind
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

internal suspend fun prepareProfileImage(
    context: Context,
    uri: Uri,
    kind: ProfileImageKind,
): ByteArray? = withContext(Dispatchers.IO) {
    val sourceBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: return@withContext null
    val (width, height, maxBytes) = when (kind) {
        ProfileImageKind.AVATAR -> Triple(250, 250, 2 * 1024 * 1024)
        ProfileImageKind.BANNER -> Triple(1370, 170, 4 * 1024 * 1024)
    }
    val orientation = sourceBytes.exifOrientation()
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
    val swapsDimensions = orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270
    val sourceWidth = if (swapsDimensions) bounds.outHeight else bounds.outWidth
    val sourceHeight = if (swapsDimensions) bounds.outWidth else bounds.outHeight
    var sampleSize = 1
    while (sourceWidth / (sampleSize * 2) >= width && sourceHeight / (sampleSize * 2) >= height) {
        sampleSize *= 2
    }
    val decoded = BitmapFactory.decodeByteArray(
        sourceBytes,
        0,
        sourceBytes.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    ) ?: return@withContext null
    val oriented = decoded.applyExifOrientation(orientation)
    if (oriented !== decoded) decoded.recycle()
    val cropped = oriented.centerCrop(width, height)
    if (cropped !== oriented) oriented.recycle()
    var quality = 94
    var output: ByteArray
    do {
        output = ByteArrayOutputStream().use { stream ->
            cropped.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }
        quality -= 8
    } while (output.size > maxBytes && quality >= 54)
    cropped.recycle()
    output.takeIf { it.size <= maxBytes }
}

private fun ByteArray.exifOrientation(): Int =
    runCatching {
        ExifInterface(ByteArrayInputStream(this)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

private fun Bitmap.applyExifOrientation(orientation: Int): Bitmap {
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                setRotate(180f)
                postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                setRotate(90f)
                postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                setRotate(-90f)
                postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
        }
    }
    if (matrix.isIdentity) return this
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private fun Bitmap.centerCrop(targetWidth: Int, targetHeight: Int): Bitmap {
    val scale = maxOf(targetWidth.toFloat() / width, targetHeight.toFloat() / height)
    val scaledWidth = (width * scale).toInt().coerceAtLeast(targetWidth)
    val scaledHeight = (height * scale).toInt().coerceAtLeast(targetHeight)
    val scaled = Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
    val left = ((scaledWidth - targetWidth) / 2).coerceAtLeast(0)
    val top = ((scaledHeight - targetHeight) / 2).coerceAtLeast(0)
    val result = Bitmap.createBitmap(scaled, left, top, targetWidth, targetHeight)
    if (result !== scaled) scaled.recycle()
    return result
}
