package su.afk.yummy.tv.source.torrent.domain.usecase

import su.afk.yummy.tv.source.torrent.domain.repository.TorrServerRepository
import javax.inject.Inject

/**
 * Узнаёт, какие раздачи уже есть в TorrServer, чтобы пометить их в списке выбора. Повторное добавление той же
 * раздачи безопасно (сервер различает их по инфо-хешу), поэтому результат носит справочный характер: при недоступном
 * сервере возвращается пустое множество.
 */
class GetKnownTorrentHashesUseCase @Inject constructor(
    private val torrServer: TorrServerRepository,
) {
    suspend operator fun invoke(): Set<String> = torrServer.knownHashes()
}
