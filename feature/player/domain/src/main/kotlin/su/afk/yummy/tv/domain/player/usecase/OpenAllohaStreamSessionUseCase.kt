package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.repository.PlayerStreamRepository
import javax.inject.Inject

/** Открывает обновляемую сессию потока Alloha для запроса встроенного плеера. */
class OpenAllohaStreamSessionUseCase @Inject constructor(
    private val repository: PlayerStreamRepository,
) {
    suspend operator fun invoke(request: PlayerStreamRequest) =
        repository.openAllohaSession(request)
}
