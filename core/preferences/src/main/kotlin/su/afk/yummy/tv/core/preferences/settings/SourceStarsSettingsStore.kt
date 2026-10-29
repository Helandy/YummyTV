package su.afk.yummy.tv.core.preferences.settings

import kotlinx.coroutines.flow.Flow

/**
 * Озвучки и балансеры, помеченные звездой: такие всегда показываются сверху списков выбора.
 * Множества содержат нормализованные ключи (`dubbingStarKey` / `balancerStarKey`).
 */
interface SourceStarsSettingsStore {

    val starredDubbings: Flow<Set<String>>
    val starredBalancers: Flow<Set<String>>

    suspend fun toggleDubbing(name: String)
    suspend fun toggleBalancer(name: String)
}
