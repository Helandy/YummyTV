package su.afk.yummy.tv.domain.player.repository

import su.afk.yummy.tv.domain.player.model.PlayerCachePurgeResult

interface PlayerPlaybackCacheRepository {
    /**
     * Полностью очищает дисковый кэш онлайн-воспроизведения. Кэш офлайн-загрузок пользователя
     * не затрагивается.
     */
    suspend fun purge(): PlayerCachePurgeResult
}
