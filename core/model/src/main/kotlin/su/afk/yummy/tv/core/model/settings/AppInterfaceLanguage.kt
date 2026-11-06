package su.afk.yummy.tv.core.model.settings

/**
 * Язык интерфейса приложения. Не путать с [YaniContentLanguage] — языком контента из API.
 *
 * [languageTag] — BCP 47-тег для подмены локали; у [SYSTEM] его нет: приложение следует языку устройства.
 */
enum class AppInterfaceLanguage(val languageTag: String?) {
    SYSTEM(null),
    RUSSIAN("ru"),
    ENGLISH("en"),
    UKRAINIAN("uk"),
    ;

    companion object {
        val DEFAULT: AppInterfaceLanguage = SYSTEM

        fun fromPreferenceValue(value: String?): AppInterfaceLanguage =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}
