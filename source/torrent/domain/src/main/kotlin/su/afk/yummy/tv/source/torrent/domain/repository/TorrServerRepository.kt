package su.afk.yummy.tv.source.torrent.domain.repository

import su.afk.yummy.tv.source.torrent.domain.model.TorrentContent
import su.afk.yummy.tv.source.torrent.domain.model.TorrentFile
import su.afk.yummy.tv.source.torrent.domain.model.TorrentStream

/** Клиент TorrServer, адрес которого пользователь указал в настройках. */
interface TorrServerRepository {
    /** Добавляет раздачу на сервер и ждёт список файлов. Ошибки сети и таймаут метаданных пробрасываются. */
    suspend fun addTorrent(link: String): TorrentContent

    /** Адрес HTTP-потока [file] раздачи [hash] и заголовки авторизации. */
    suspend fun streamFor(hash: String, file: TorrentFile): TorrentStream
}
