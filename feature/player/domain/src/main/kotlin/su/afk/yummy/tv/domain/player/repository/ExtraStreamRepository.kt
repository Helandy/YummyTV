package su.afk.yummy.tv.domain.player.repository

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult

/**
 * Точка расширения: модуль-источник превращает свою ссылку `source://…` в прямой поток.
 * Здесь же допустимо показать рекламу или запросить доступ: разрешение потока проходят все входы в плеер.
 */
interface ExtraStreamRepository {
    fun supports(url: String): Boolean

    suspend fun resolve(url: String): PlayerStreamResolveResult
}
