package su.afk.yummy.tv.core.utils.formatting

import java.util.Locale

/** Сервер отдаёт название типа уже локализованным; ключ — название без регистра, значение — короткая форма. */
private val ShortAnimeTypeNames = mapOf(
    "полнометражный фильм" to "Фильм",
    "повнометражний фільм" to "Фільм",
    "full-length movie" to "Movie",
    "feature film" to "Movie",
)

/**
 * Укорачивает длинное название типа тайтла («Полнометражный фильм» → «Фильм»); остальные названия,
 * в том числе null, возвращает без изменений.
 */
fun String?.toShortAnimeTypeName(): String? {
    val name = this ?: return null
    return ShortAnimeTypeNames[name.trim().lowercase(Locale.ROOT)] ?: name
}
