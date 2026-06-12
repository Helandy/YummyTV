package su.afk.yummy.tv.domain.player.repository

import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult

interface PlayerStreamRepository {
    suspend fun resolve(request: PlayerStreamRequest): PlayerStreamResolveResult
}
