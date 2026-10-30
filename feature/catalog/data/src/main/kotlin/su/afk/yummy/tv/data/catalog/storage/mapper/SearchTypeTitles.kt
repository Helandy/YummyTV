package su.afk.yummy.tv.data.catalog.storage.mapper

import su.afk.yummy.tv.core.model.settings.YaniContentLanguage

/**
 * Типы тайтлов для фильтра. Список у yani не меняется, а `/anime/catalog` ради него отдаёт ~325 КБ
 * и отвечает до 20 секунд (limit игнорируется), поэтому названия зашиты по языку контента.
 */
private val SearchTypeTitlesByLanguage: Map<String, List<Pair<String, String>>> = mapOf(
    YaniContentLanguage.RUSSIAN.apiCode to listOf(
        "tv" to "Сериал",
        "ona" to "ONA",
        "ova" to "OVA",
        "movie" to "Фильм",
        "special" to "Спешл",
    ),
    YaniContentLanguage.UKRAINIAN.apiCode to listOf(
        "tv" to "Серіал",
        "ona" to "ONA",
        "ova" to "OVA",
        "movie" to "Фільм",
        "special" to "Спешл",
    ),
    YaniContentLanguage.ENGLISH.apiCode to listOf(
        "tv" to "TV",
        "ona" to "ONA",
        "ova" to "OVA",
        "movie" to "Movie",
        "special" to "Special",
    ),
)

/** Пары «id для API → название» в порядке показа; неизвестный язык получает русские названия. */
internal fun searchTypeTitles(languageCode: String): List<Pair<String, String>> =
    SearchTypeTitlesByLanguage[languageCode]
        ?: SearchTypeTitlesByLanguage.getValue(YaniContentLanguage.DEFAULT.apiCode)
