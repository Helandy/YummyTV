package su.afk.yummy.tv.source.torrent.domain.usecase

import su.afk.yummy.tv.source.torrent.domain.model.TorrentSelection
import su.afk.yummy.tv.source.torrent.domain.model.TorrentStream
import su.afk.yummy.tv.source.torrent.domain.repository.TorrServerRepository
import javax.inject.Inject

/**
 * Превращает выбор пользователя в адрес потока TorrServer, который играет обычный плеер. Перед выдачей ждёт, пока
 * сервер начнёт отдавать данные: иначе плеер на медленном старте раздачи упирается в свой таймаут и зависает.
 * Если данные не пошли, возвращает `null`.
 */
class BuildTorrentStreamUseCase @Inject constructor(
    private val torrServer: TorrServerRepository,
) {
    suspend operator fun invoke(selection: TorrentSelection): TorrentStream? {
        val stream = torrServer.streamFor(selection.hash, selection.file)
        return stream.takeIf { torrServer.awaitStreamData(it) }
    }
}
