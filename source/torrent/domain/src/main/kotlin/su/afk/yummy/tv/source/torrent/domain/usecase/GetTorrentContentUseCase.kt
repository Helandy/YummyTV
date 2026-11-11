package su.afk.yummy.tv.source.torrent.domain.usecase

import su.afk.yummy.tv.source.torrent.domain.model.TorrentContent
import su.afk.yummy.tv.source.torrent.domain.model.TorrentFile
import su.afk.yummy.tv.source.torrent.domain.model.episodeMatchScore
import su.afk.yummy.tv.source.torrent.domain.model.isVideo
import su.afk.yummy.tv.source.torrent.domain.repository.TorrServerRepository
import javax.inject.Inject

/**
 * Добавляет раздачу в TorrServer и возвращает только видеофайлы: файл серии [episode] (если его удалось
 * распознать по номеру в имени) идёт первым, остальные по имени. Без видеофайлов список пуст.
 */
class GetTorrentContentUseCase @Inject constructor(
    private val torrServer: TorrServerRepository,
) {
    suspend operator fun invoke(link: String, episode: String): TorrentContent {
        val content = torrServer.addTorrent(link)
        val videos = content.files
            .filter { it.isVideo }
            .sortedWith(
                compareByDescending<TorrentFile> {
                    it.episodeMatchScore(episode)
                }.thenBy { it.path.lowercase() },
            )
        return content.copy(files = videos)
    }
}
