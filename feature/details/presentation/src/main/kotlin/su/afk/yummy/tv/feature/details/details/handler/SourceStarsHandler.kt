package su.afk.yummy.tv.feature.details.details.handler

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.preferences.settings.SourceStarsSettingsStore
import javax.inject.Inject

/** Звёзды озвучек и балансеров для списков выбора на экранах тайтла. */
internal class SourceStarsHandler @Inject constructor(
    private val store: SourceStarsSettingsStore,
) {
    val starredDubbings: Flow<Set<String>> get() = store.starredDubbings
    val starredBalancers: Flow<Set<String>> get() = store.starredBalancers

    suspend fun toggleDubbing(name: String) = store.toggleDubbing(name)

    suspend fun toggleBalancer(name: String) = store.toggleBalancer(name)
}
