package su.afk.yummy.tv.data.player.extractor.vk

import java.nio.charset.Charset

private val VK_PAGE_CHARSET: Charset = Charset.forName("windows-1251")
private val VK_MESSAGE_PATTERN = Regex(
    """id="video_ext_msg"[^>]*>(.*?)</div>""",
    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
)
private val HTML_TAG_PATTERN = Regex("<[^>]+>")
private val WHITESPACE_PATTERN = Regex("\\s+")

/** Фрагмент текста VK, по которому сообщение распознаётся как «видеофайл не найден». */
private const val VK_NOT_FOUND_FRAGMENT = "не найден"

/**
 * Достаёт текст сообщения VK из страницы `video_ext.php`, которая приходит в windows-1251.
 * Клиент декодирует тело как UTF-8, поэтому русский текст читается только из сырых байтов.
 */
internal fun ByteArray.vkUnavailableMessage(): String? =
    VK_MESSAGE_PATTERN.find(String(this, VK_PAGE_CHARSET))
        ?.groupValues
        ?.get(1)
        ?.replace(HTML_TAG_PATTERN, " ")
        ?.replace(WHITESPACE_PATTERN, " ")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

/** VK сообщает, что видеофайла нет (удалён или никогда не существовал). */
internal fun String.isVkVideoNotFoundMessage(): Boolean =
    contains(VK_NOT_FOUND_FRAGMENT, ignoreCase = true)
