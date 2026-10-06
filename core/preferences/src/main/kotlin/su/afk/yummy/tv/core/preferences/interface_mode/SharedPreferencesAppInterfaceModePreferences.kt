package su.afk.yummy.tv.core.preferences.interface_mode

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class SharedPreferencesAppInterfaceModePreferences @Inject constructor(
    @ApplicationContext context: Context,
) : AppInterfaceModePreferences {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override val selectedMode: AppInterfaceMode?
        get() = preferences.getString(SELECTED_MODE_KEY, null)?.let { value ->
            runCatching { AppInterfaceMode.valueOf(value) }.getOrNull()
        }

    override fun select(mode: AppInterfaceMode) {
        preferences.edit().putString(SELECTED_MODE_KEY, mode.name).apply()
    }

    override fun consumePlayerSetupPending(): Boolean {
        if (preferences.getBoolean(PLAYER_SETUP_SHOWN_KEY, false)) return false
        preferences.edit().putBoolean(PLAYER_SETUP_SHOWN_KEY, true).apply()
        return true
    }

    private companion object {
        const val PREFERENCES_NAME = "app_interface_mode"
        const val SELECTED_MODE_KEY = "selected_mode"
        const val PLAYER_SETUP_SHOWN_KEY = "player_setup_shown"
    }
}
