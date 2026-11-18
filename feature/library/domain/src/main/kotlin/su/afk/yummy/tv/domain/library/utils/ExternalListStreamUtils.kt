package su.afk.yummy.tv.domain.library.utils

import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.model.ExternalListParseError
import su.afk.yummy.tv.domain.library.model.ExternalListParseException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException

/** Максимальный размер входного файла до распаковки (10 МБ). */
const val MAX_IMPORT_RAW_BYTES = 10 * 1024 * 1024

/** Максимальный размер входного файла после распаковки GZIP (20 МБ, защита от zip-бомб). */
const val MAX_IMPORT_DECOMPRESSED_BYTES = 20 * 1024 * 1024

private const val GZIP_MAGIC_FIRST = 0x1F
private const val GZIP_MAGIC_SECOND = 0x8B
private const val UTF8_BOM = '\uFEFF'
private const val IO_BUFFER_SIZE = 8 * 1024

/**
 * Распаковывает входной массив (если это GZIP — стандартный `.xml.gz` экспорта MyAnimeList),
 * декодирует UTF-8 и снимает BOM, соблюдая лимит [maxDecompressedBytes].
 */
fun ByteArray.decodeExternalListText(
    maxDecompressedBytes: Int = MAX_IMPORT_DECOMPRESSED_BYTES,
): String {
    if (isEmpty()) {
        throw ExternalListParseException(ExternalListParseError.EMPTY_FILE)
    }
    val rawBytes = if (isGzipCompressed()) {
        gunzipBounded(maxDecompressedBytes)
    } else {
        if (size > maxDecompressedBytes) {
            throw ExternalListParseException(ExternalListParseError.FILE_TOO_LARGE)
        }
        this
    }
    if (rawBytes.isEmpty()) {
        throw ExternalListParseException(ExternalListParseError.EMPTY_FILE)
    }
    val decoded = rawBytes.toString(Charsets.UTF_8).trimStart(UTF8_BOM)
    if (decoded.isBlank()) {
        throw ExternalListParseException(ExternalListParseError.EMPTY_FILE)
    }
    return decoded
}

/** Определяет формат внешнего списка по первому значимому символу содержимого. */
fun detectExternalListFormat(content: String): ExternalListFormat {
    val firstChar = content.firstOrNull { !it.isWhitespace() && it != UTF8_BOM }
        ?: throw ExternalListParseException(ExternalListParseError.EMPTY_FILE)
    return when (firstChar) {
        '<' -> ExternalListFormat.MAL_XML
        '[' -> ExternalListFormat.SHIKIMORI_JSON
        else -> throw ExternalListParseException(ExternalListParseError.UNSUPPORTED_FORMAT)
    }
}

private fun ByteArray.isGzipCompressed(): Boolean =
    size >= 2 &&
        (this[0].toInt() and 0xFF) == GZIP_MAGIC_FIRST &&
        (this[1].toInt() and 0xFF) == GZIP_MAGIC_SECOND

private fun ByteArray.gunzipBounded(maxBytes: Int): ByteArray {
    try {
        GZIPInputStream(ByteArrayInputStream(this)).use { input ->
            val output = ByteArrayOutputStream(minOf(size * 4, maxBytes))
            val buffer = ByteArray(IO_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > maxBytes) {
                    throw ExternalListParseException(ExternalListParseError.FILE_TOO_LARGE)
                }
                output.write(buffer, 0, read)
            }
            return output.toByteArray()
        }
    } catch (error: ExternalListParseException) {
        throw error
    } catch (error: ZipException) {
        throw ExternalListParseException(ExternalListParseError.MALFORMED_CONTENT, error)
    } catch (error: Exception) {
        throw ExternalListParseException(ExternalListParseError.MALFORMED_CONTENT, error)
    }
}
