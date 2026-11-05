package su.afk.yummy.tv.data.details.utils

import java.util.Locale

/**
 * Язык названий и описаний серий для YummyTV API: язык интерфейса приложения (ru, uk, en); для остальных локалей русский,
 * как у сервера по умолчанию.
 */
internal fun episodeTextLanguage(locale: Locale = Locale.getDefault()): String =
    when (locale.language) {
        "uk" -> "uk"
        "en" -> "en"
        else -> "ru"
    }
