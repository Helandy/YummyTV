package su.afk.yummy.tv.core.preferences.settings.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.model.settings.SupportPromptSnapshot
import su.afk.yummy.tv.core.preferences.settings.AppLifecycleSettingsStore
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.betaUpdatesEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.lastSeenAnnouncementIdKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.lastStartedVersionCodeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.notificationPermissionRequestedKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.supportPromptDismissedKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.supportPromptFirstInstallTimeMsKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.watchNextEnabledKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataStoreAppLifecycleSettingsStore @Inject constructor(
    private val store: SettingsDataStore,
) : AppLifecycleSettingsStore {

    override val watchNextEnabled: Flow<Boolean> = store.boolean(watchNextEnabledKey, true)

    override val betaUpdatesEnabled: Flow<Boolean> = store.boolean(betaUpdatesEnabledKey, false)

    override val supportPromptSnapshot: Flow<SupportPromptSnapshot> = store.data.map { prefs ->
        SupportPromptSnapshot(
            dismissed = prefs[supportPromptDismissedKey] ?: false,
            firstEligibleTimeMs = prefs[supportPromptFirstInstallTimeMsKey]
                ?: System.currentTimeMillis(),
        )
    }

    override val lastSeenAnnouncementId: Flow<String> = store.string(lastSeenAnnouncementIdKey)

    override val notificationPermissionRequested: Flow<Boolean> =
        store.boolean(notificationPermissionRequestedKey, false)

    override suspend fun setWatchNextEnabled(enabled: Boolean) =
        store.setBoolean(watchNextEnabledKey, enabled)

    override suspend fun setBetaUpdatesEnabled(enabled: Boolean) =
        store.setBoolean(betaUpdatesEnabledKey, enabled)

    override suspend fun ensureSupportPromptInstallTimeInitialized() {
        store.edit { prefs ->
            if (prefs[supportPromptFirstInstallTimeMsKey] == null) {
                prefs[supportPromptFirstInstallTimeMsKey] = System.currentTimeMillis()
            }
        }
    }

    override suspend fun dismissSupportPrompt() =
        store.setBoolean(supportPromptDismissedKey, true)

    override suspend fun markAnnouncementSeen(id: String) {
        store.edit { prefs -> prefs[lastSeenAnnouncementIdKey] = id }
    }

    override suspend fun markNotificationPermissionRequested() =
        store.setBoolean(notificationPermissionRequestedKey, true)

    override suspend fun markStartedVersion(versionCode: Int): Boolean {
        var isFreshVersion = true
        store.edit { prefs ->
            val lastStartedVersionCode = prefs[lastStartedVersionCodeKey]
            isFreshVersion = lastStartedVersionCode != versionCode
            if (isFreshVersion && prefs[supportPromptDismissedKey] != true) {
                prefs[supportPromptFirstInstallTimeMsKey] = System.currentTimeMillis()
            }
            prefs[lastStartedVersionCodeKey] = versionCode
        }
        return isFreshVersion
    }
}
