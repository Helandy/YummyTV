package su.afk.yummy.tv.core.preferences.interface_language

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import su.afk.yummy.tv.core.model.settings.AppInterfaceLanguage
import java.util.Locale

/**
 * Применяет выбранный язык интерфейса к контексту. Вызывается из `attachBaseContext`
 * Application и Activity: там Hilt ещё недоступен, поэтому язык читается напрямую из SharedPreferences.
 * Своя подмена локали, а не AppCompatDelegate: она одинаково работает на любых версиях Android,
 * в том числе на ТВ-приставках ниже 13, где системный выбор языка приложения отсутствует.
 */
object AppInterfaceLanguageContext {

    internal const val PREFERENCES_NAME = "app_interface_language"
    internal const val LANGUAGE_KEY = "language"

    internal fun read(context: Context): AppInterfaceLanguage =
        AppInterfaceLanguage.fromPreferenceValue(
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(LANGUAGE_KEY, null),
        )

    /** Контекст с локалью выбранного языка; для «как в системе» возвращает [base] без изменений. */
    fun wrap(base: Context): Context {
        val tag = read(base).languageTag ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocales(LocaleList(locale))
        return base.createConfigurationContext(configuration)
    }
}
