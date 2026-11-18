// Part of YummyTV — https://github.com/Helandy/YummyTV (CC BY-NC 4.0, see NOTICE)
package su.afk.yummy.tv.feature.player.extractor

import android.content.Context
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult

interface PlayerStreamExtractor {
    fun supports(url: String): Boolean

    /** Экстрактор сам сообщает свои шаги через `request.onStatus`; сетевой слой для него отдаёт только подробности запросов. */
    val reportsOwnProgress: Boolean get() = false

    /**
     * Закрывает живую сессию воспроизведения (сокет, отчёты о просмотре), если экстрактор её держит.
     * Возвращает `true`, если что-то было открыто: закэшированные потоки такого источника без сессии
     * больше не годятся.
     */
    fun releaseSession(): Boolean = false

    suspend fun extract(
        request: PlayerStreamRequest,
        context: Context,
    ): PlayerStreamResolveResult
}
