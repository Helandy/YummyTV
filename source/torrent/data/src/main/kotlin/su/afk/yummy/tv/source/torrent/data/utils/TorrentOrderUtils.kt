package su.afk.yummy.tv.source.torrent.data.utils

import su.afk.yummy.tv.source.torrent.domain.model.TorrentResult
import su.afk.yummy.tv.source.torrent.domain.model.infoHash

/**
 * Порядок списка выбора раздачи: сначала раздача, которая сейчас играет ([currentHash]), затем остальные,
 * уже добавленные в TorrServer ([knownHashes]), затем прочие. Внутри групп сохраняется порядок индексатора.
 */
internal fun List<TorrentResult>.pinServerTorrentsFirst(
    currentHash: String?,
    knownHashes: Set<String>,
): List<TorrentResult> = sortedBy { result ->
    val hash = result.infoHash
    when {
        hash == null -> 2
        hash == currentHash -> 0
        hash in knownHashes -> 1
        else -> 2
    }
}
