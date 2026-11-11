package su.afk.yummy.tv.source.torrent.domain.repository

import su.afk.yummy.tv.source.torrent.domain.model.TorrentResult

/** Индексатор раздач (JacRed или Jackett, по настройке пользователя). */
interface TorrentIndexerRepository {
    /** Раздачи по текстовому запросу; пустой список — ничего не нашлось. Ошибки сети пробрасываются. */
    suspend fun search(query: String): List<TorrentResult>
}
