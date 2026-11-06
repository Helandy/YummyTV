package su.afk.yummy.tv.core.preferences.interface_language

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.core.model.settings.AppInterfaceLanguage
import javax.inject.Inject

internal class SharedPreferencesAppInterfaceLanguagePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppInterfaceLanguagePreferences {

    override val selectedLanguage: AppInterfaceLanguage
        get() = AppInterfaceLanguageContext.read(context)

    override fun select(language: AppInterfaceLanguage) {
        // commit(), а не apply(): сразу за выбором следует холодный рестарт процесса,
        // и асинхронная запись могла бы не успеть.
        context.getSharedPreferences(AppInterfaceLanguageContext.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(AppInterfaceLanguageContext.LANGUAGE_KEY, language.name)
            .commit()
    }
}
