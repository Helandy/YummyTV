package su.afk.yummy.tv.data.videodownload.utils

import java.util.Locale

/** Папка тайтла: с запасом от 255 байт — провайдер может дописать свой суффикс « (N)». */
internal const val MAX_DIRECTORY_NAME_BYTES = 180

/** Имя файла серии вместе с расширением. */
internal const val MAX_FILE_NAME_BYTES = 200

/**
 * Приводит строку к имени, которое примет SAF-провайдер: вычищает запрещённые на FAT символы
 * и укорачивает результат до [maxBytes] **байт** в UTF-8.
 *
 * Считать символы нельзя: FAT/exFAT и `FileUtils.trimFilename` внутри `ExternalStorageProvider`
 * ограничивают имя байтами, а кириллица занимает по два байта на символ — иначе провайдер режет
 * имя сам (и тогда его не найти по имени) либо отдаёт `Operation not permitted`.
 */
internal fun String.toSafeSafName(maxBytes: Int): String =
    replace(FORBIDDEN_FILE_NAME_CHARS, " ")
        .replace(CONTROL_CHARS, " ")
        .replace(WHITESPACE, " ")
        .trim(*TRIMMED_EDGE_CHARS)
        .truncateToUtf8Bytes(maxBytes)
        // Обрезка могла оставить точку или пробел на конце — такое имя FAT не принимает
        .trim(*TRIMMED_EDGE_CHARS)

/** Длина строки в UTF-8: расширение и разделители тоже должны влезть в байтовый лимит. */
internal fun String.utf8Size(): Int = toByteArray(Charsets.UTF_8).size

/** Обрезает строку до [maxBytes] байт в UTF-8, не разрывая surrogate-пары. */
internal fun String.truncateToUtf8Bytes(maxBytes: Int): String {
    if (maxBytes <= 0) return ""
    if (utf8Size() <= maxBytes) return this
    val builder = StringBuilder()
    var bytes = 0
    var index = 0
    while (index < length) {
        val codePoint = codePointAt(index)
        val charCount = Character.charCount(codePoint)
        val chunk = substring(index, index + charCount)
        val chunkBytes = chunk.utf8Size()
        if (bytes + chunkBytes > maxBytes) break
        builder.append(chunk)
        bytes += chunkBytes
        index += charCount
    }
    return builder.toString()
}

/**
 * Ключ для сравнения имён документов. Провайдер вправе вернуть имя не тем, каким мы его отдали:
 * FAT регистронезависим, длинное имя может оказаться укороченным, а к дублю дописывается « (N)».
 * Строгое `==` по `COLUMN_DISPLAY_NAME` из-за этого не находит уже созданную папку и плодит дубли.
 */
internal fun String.safNameKey(): String =
    replace(WHITESPACE, " ")
        .trim(*TRIMMED_EDGE_CHARS)
        .replace(DUPLICATE_SUFFIX, "")
        .trim(*TRIMMED_EDGE_CHARS)
        .lowercase(Locale.ROOT)

/**
 * Похоже ли имя существующего документа на укороченный провайдером вариант [fullName].
 * Нужно, чтобы подхватить папки, созданные до перехода на байтовую обрезку.
 */
internal fun String.isTruncatedSafNameOf(fullName: String): Boolean {
    val key = safNameKey()
    return key.length >= MIN_TRUNCATED_MATCH_LENGTH && fullName.safNameKey().startsWith(key)
}

/** Ниже этой длины префиксное совпадение слишком легко поймать на чужой папке. */
private const val MIN_TRUNCATED_MATCH_LENGTH = 40

private val FORBIDDEN_FILE_NAME_CHARS = Regex("""[/\\:*?"<>|]""")
private val CONTROL_CHARS = Regex("[\\u0000-\\u001F\\u007F]")
private val WHITESPACE = Regex("\\s+")
private val DUPLICATE_SUFFIX = Regex("""\s*\(\d+\)$""")
private val TRIMMED_EDGE_CHARS = charArrayOf(' ', '.', '_')
