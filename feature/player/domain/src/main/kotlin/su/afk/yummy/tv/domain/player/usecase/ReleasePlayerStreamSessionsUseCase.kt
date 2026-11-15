package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.domain.player.repository.PlayerStreamRepository
import javax.inject.Inject

/**
 * Завершает сессии воспроизведения источников при уходе из плеера.
 *
 * Часть источников (Alloha) держит на время просмотра WebSocket и периодические отчёты о просмотре.
 * Без явного закрытия они живут до следующего запуска того же источника и в фоне будят радиомодуль.
 */
class ReleasePlayerStreamSessionsUseCase @Inject constructor(
    private val repository: PlayerStreamRepository,
) {
    operator fun invoke() = repository.releaseStreamSessions()
}
