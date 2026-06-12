package su.afk.yummy.tv.core.preferences.settings

import android.content.Context
import android.os.Build

enum class YaniContentLanguage(val apiCode: String) {
    RUSSIAN("ru"),
    ENGLISH("en"),
    UKRAINIAN("uk"),
    ;

    companion object {
        val DEFAULT: YaniContentLanguage = RUSSIAN

        fun fromPreferenceValue(value: String?): YaniContentLanguage? {
            if (value.isNullOrBlank()) return null
            return entries.firstOrNull { it.name == value }
                ?: entries.firstOrNull { it.apiCode == value }
        }

        fun fromSystemLocale(context: Context): YaniContentLanguage {
            val languageCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales.get(0)?.language
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale?.language
            }
            return entries.firstOrNull { it.apiCode.equals(languageCode, ignoreCase = true) }
                ?: DEFAULT
        }
    }
}

fun String.withYaniContentLanguage(language: YaniContentLanguage): String =
    "${this}_lang_${language.apiCode}"
