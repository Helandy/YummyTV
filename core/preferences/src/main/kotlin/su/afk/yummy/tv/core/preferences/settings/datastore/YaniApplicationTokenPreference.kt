package su.afk.yummy.tv.core.preferences.settings.datastore

import androidx.datastore.preferences.core.Preferences
import su.afk.yummy.tv.core.model.settings.YaniApplicationToken
import su.afk.yummy.tv.core.model.settings.YaniApplicationTokenState
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniApplicationTokenKey

internal const val DEFAULT_YANI_APPLICATION_TOKEN = YaniApplicationToken.DEFAULT

/** Токен приложения: пользовательский, если задан, иначе встроенный по умолчанию. */
internal fun Preferences.yaniApplicationToken(): String =
    this[yaniApplicationTokenKey]?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_YANI_APPLICATION_TOKEN

/** Отличает пользовательский токен от встроенного — на этом строится подсказка в настройках. */
internal fun Preferences.yaniApplicationTokenState(): YaniApplicationTokenState = if (yaniApplicationToken() != DEFAULT_YANI_APPLICATION_TOKEN) {
    YaniApplicationTokenState.CUSTOM
} else {
    YaniApplicationTokenState.DEFAULT
}
