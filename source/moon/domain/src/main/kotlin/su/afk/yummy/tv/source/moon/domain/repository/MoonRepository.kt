package su.afk.yummy.tv.source.moon.domain.repository

import su.afk.yummy.tv.source.moon.domain.model.MoonStreamsResult

interface MoonRepository {
    /**
     * Поток со страницы плеера `https://moonanime.art/iframe/{id}/` или её зеркала `.net` / `.vip` (параметры адреса, например `?player=animeon.club`, отбрасываются).
     * [MoonStreamsResult.NotFound], если страницы нет, [MoonStreamsResult.Failed], если адрес чужой или страница изменилась и поток в ней
     * не найден. Сбой сети уходит исключением.
     */
    suspend fun getStreams(pageUrl: String): MoonStreamsResult
}
