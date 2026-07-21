package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.PlayerStreamRepository
import javax.inject.Inject

/** Преобразует запрос встроенного плеера в поток для воспроизведения или структурированную ошибку. */
class ResolvePlayerStreamUseCase @Inject constructor(private val repository: PlayerStreamRepository) {
    suspend operator fun invoke(request: PlayerStreamRequest): PlayerStreamResolveResult =
        repository.resolve(request)
}
