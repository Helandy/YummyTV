package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.PlayerStreamRepository

class ResolvePlayerStreamUseCase(private val repository: PlayerStreamRepository) {
    suspend operator fun invoke(request: PlayerStreamRequest): PlayerStreamResolveResult =
        repository.resolve(request)
}
