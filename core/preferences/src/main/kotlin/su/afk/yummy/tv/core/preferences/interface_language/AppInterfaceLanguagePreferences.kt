package su.afk.yummy.tv.core.preferences.interface_language

import su.afk.yummy.tv.core.model.settings.AppInterfaceLanguage

/** Синхронное хранилище: читается в `attachBaseContext` до Hilt и setContent, поэтому не DataStore. */
interface AppInterfaceLanguagePreferences {

    val selectedLanguage: AppInterfaceLanguage

    fun select(language: AppInterfaceLanguage)
}
