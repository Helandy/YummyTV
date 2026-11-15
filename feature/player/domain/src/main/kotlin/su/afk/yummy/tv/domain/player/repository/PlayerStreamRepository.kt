package su.afk.yummy.tv.domain.player.repository

import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult

interface PlayerStreamRepository {
    suspend fun resolve(request: PlayerStreamRequest): PlayerStreamResolveResult

    /** Забывает закэшированный поток источника: его ссылки больше не ведут на живой узел CDN. */
    fun invalidateResolveCache(iframeUrl: String)

    /**
     * Закрывает живые сессии воспроизведения источников (сокеты, отчёты о просмотре) при уходе из
     * плеера и забывает их закэшированные потоки: без сессии те ссылки уже не играют.
     */
    fun releaseStreamSessions()
}
