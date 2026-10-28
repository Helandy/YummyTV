package su.afk.yummy.tv.source.fenix.domain.repository

import su.afk.yummy.tv.source.fenix.domain.model.FenixStreamsResult

interface FenixRepository {
    /**
     * Поток со страницы плеера `https://fenixplay.xyz/vod/{токен}` (серия и озвучка заданы токеном) или
     * `https://fenixplay.xyz/embed/{токен}?ep={серия}&edition={издание}` (серия берётся из параметров, озвучка — по названию
     * [track], а если её нет, играет оригинальная дорожка). [FenixStreamsResult.NotFound], если страницы, серии или адреса
     * плеера нет. Сбой сети уходит исключением.
     */
    suspend fun getStreams(pageUrl: String, track: String?): FenixStreamsResult
}
