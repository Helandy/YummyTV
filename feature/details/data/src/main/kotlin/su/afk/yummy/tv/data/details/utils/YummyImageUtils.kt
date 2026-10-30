package su.afk.yummy.tv.data.details.utils

import su.afk.yummy.tv.data.details.dto.YummyImageDto

private const val ORIGINAL_SEGMENT = "/original/"

/**
 * Ссылка на картинку TMDB нужного размера (`w780`, `w1280`…). Оригинал весит мегабайты, а задник и кадры показываем
 * сжатыми, поэтому размер подставляется в путь CDN. Если ссылка не в форме `…/original/…`, отдаётся как есть.
 */
internal fun YummyImageDto.sizedUrl(size: String): String? {
    if (url.isBlank()) return null
    val base = url.substringBefore(ORIGINAL_SEGMENT, missingDelimiterValue = "")
    if (base.isEmpty() || filePath.isBlank()) return url
    return "$base/$size$filePath"
}
