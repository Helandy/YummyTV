package su.afk.yummy.tv.core.preferences.settings.datastore

import androidx.datastore.preferences.core.Preferences
import su.afk.yummy.tv.core.model.settings.WatchedThresholds
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.watchedLongRemainingMinutesKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.watchedMediumRemainingMinutesKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.watchedShortRemainingMinutesKey

internal fun Preferences.watchedThresholds(): WatchedThresholds =
    WatchedThresholds(
        shortMinutes = this[watchedShortRemainingMinutesKey]
            ?: WatchedThresholds.DEFAULT_SHORT_MINUTES,
        mediumMinutes = this[watchedMediumRemainingMinutesKey]
            ?: WatchedThresholds.DEFAULT_MEDIUM_MINUTES,
        longMinutes = this[watchedLongRemainingMinutesKey]
            ?: WatchedThresholds.DEFAULT_LONG_MINUTES,
    ).coerced()
