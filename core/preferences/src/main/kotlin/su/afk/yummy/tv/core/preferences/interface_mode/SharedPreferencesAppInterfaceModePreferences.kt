package su.afk.yummy.tv.core.preferences.interface_mode

import android.content.Context

internal class SharedPreferencesAppInterfaceModePreferences(
    context: Context,
) : AppInterfaceModePreferences {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override val selectedMode: AppInterfaceMode?
        get() = preferences.getString(SELECTED_MODE_KEY, null)?.let { value ->
            runCatching { AppInterfaceMode.valueOf(value) }.getOrNull()
        }

    override fun select(mode: AppInterfaceMode) {
        preferences.edit().putString(SELECTED_MODE_KEY, mode.name).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "app_interface_mode"
        const val SELECTED_MODE_KEY = "selected_mode"
    }
}
