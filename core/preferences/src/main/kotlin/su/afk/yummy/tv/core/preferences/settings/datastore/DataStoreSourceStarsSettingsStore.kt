package su.afk.yummy.tv.core.preferences.settings.datastore

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.common.player.balancerStarKey
import su.afk.yummy.tv.core.common.player.dubbingStarKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.starredBalancersKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.starredDubbingsKey
import su.afk.yummy.tv.core.preferences.settings.SourceStarsSettingsStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataStoreSourceStarsSettingsStore @Inject constructor(
    private val store: SettingsDataStore,
) : SourceStarsSettingsStore {

    override val starredDubbings: Flow<Set<String>> =
        store.data.map { it[starredDubbingsKey].orEmpty() }.distinctUntilChanged()

    override val starredBalancers: Flow<Set<String>> =
        store.data.map { it[starredBalancersKey].orEmpty() }.distinctUntilChanged()

    override suspend fun toggleDubbing(name: String) {
        toggle(starredDubbingsKey, name.dubbingStarKey())
    }

    override suspend fun toggleBalancer(name: String) {
        toggle(starredBalancersKey, name.balancerStarKey())
    }

    private suspend fun toggle(key: Preferences.Key<Set<String>>, value: String) {
        if (value.isEmpty()) return
        store.edit { prefs ->
            val current = prefs[key].orEmpty()
            prefs[key] = if (value in current) current - value else current + value
        }
    }
}
