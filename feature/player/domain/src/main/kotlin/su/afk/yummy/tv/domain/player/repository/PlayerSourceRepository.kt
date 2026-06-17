package su.afk.yummy.tv.domain.player.repository

import su.afk.yummy.tv.domain.player.model.PlayerSourceData

interface PlayerSourceRepository {
    suspend fun getSources(
        animeId: Int,
        forceRefreshVideos: Boolean = false,
    ): PlayerSourceData
}
