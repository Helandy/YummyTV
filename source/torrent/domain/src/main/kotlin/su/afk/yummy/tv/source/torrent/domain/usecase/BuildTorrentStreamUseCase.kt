package su.afk.yummy.tv.source.torrent.domain.usecase

import su.afk.yummy.tv.source.torrent.domain.model.TorrentSelection
import su.afk.yummy.tv.source.torrent.domain.model.TorrentStream
import su.afk.yummy.tv.source.torrent.domain.repository.TorrServerRepository
import javax.inject.Inject

/** Превращает выбор пользователя в адрес потока TorrServer, который играет обычный плеер. */
class BuildTorrentStreamUseCase @Inject constructor(
    private val torrServer: TorrServerRepository,
) {
    suspend operator fun invoke(selection: TorrentSelection): TorrentStream =
        torrServer.streamFor(selection.hash, selection.file)
}
