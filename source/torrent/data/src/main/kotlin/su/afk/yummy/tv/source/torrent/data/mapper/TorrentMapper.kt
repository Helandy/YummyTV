package su.afk.yummy.tv.source.torrent.data.mapper

import su.afk.yummy.tv.source.torrent.data.dto.JackettResponseDto
import su.afk.yummy.tv.source.torrent.data.dto.JacRedTorrentDto
import su.afk.yummy.tv.source.torrent.data.dto.TorrServerTorrentDto
import su.afk.yummy.tv.source.torrent.domain.model.TorrentContent
import su.afk.yummy.tv.source.torrent.domain.model.TorrentFile
import su.afk.yummy.tv.source.torrent.domain.model.TorrentProgress
import su.afk.yummy.tv.source.torrent.domain.model.TorrentResult

/** Раздачи без ссылки (ни magnet, ни `.torrent`) отбрасываются: TorrServer ничего не сможет с ними сделать. */
internal fun List<JacRedTorrentDto>.toJacRedDomain(): List<TorrentResult> = mapNotNull { dto ->
    val link = dto.magnet?.takeIf { it.isNotBlank() } ?: dto.url?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
    TorrentResult(
        title = dto.title.orEmpty().ifBlank { link },
        sizeBytes = dto.size ?: 0L,
        seeders = dto.sid ?: 0,
        peers = dto.pir ?: 0,
        tracker = dto.tracker.orEmpty(),
        link = link,
    )
}

internal fun JackettResponseDto.toDomain(): List<TorrentResult> = results.mapNotNull { dto ->
    val link = dto.magnetUri?.takeIf { it.isNotBlank() } ?: dto.link?.takeIf { it.isNotBlank() }
        ?: return@mapNotNull null
    TorrentResult(
        title = dto.title.orEmpty().ifBlank { link },
        sizeBytes = dto.size ?: 0L,
        seeders = dto.seeders ?: 0,
        peers = dto.peers ?: 0,
        tracker = dto.tracker.orEmpty(),
        link = link,
    )
}

internal fun TorrServerTorrentDto.toProgress(): TorrentProgress = TorrentProgress(
    activePeers = activePeers ?: 0,
    totalPeers = totalPeers ?: 0,
    seeders = connectedSeeders ?: 0,
)

/** `null`, если сервер не вернул хеш. Пустой `files` означает «метаданные ещё не получены». */
internal fun TorrServerTorrentDto.toDomain(): TorrentContent? {
    val hash = hash?.takeIf { it.isNotBlank() } ?: return null
    return TorrentContent(
        hash = hash,
        title = title.orEmpty(),
        files = fileStats.orEmpty().map { TorrentFile(index = it.id, path = it.path, sizeBytes = it.length) },
    )
}
