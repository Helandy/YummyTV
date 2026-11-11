package su.afk.yummy.tv.source.torrent.domain.repository

import su.afk.yummy.tv.source.torrent.domain.model.TorrentContent
import su.afk.yummy.tv.source.torrent.domain.model.TorrentFile
import su.afk.yummy.tv.source.torrent.domain.model.TorrentProgress
import su.afk.yummy.tv.source.torrent.domain.model.TorrentStream

/** Клиент TorrServer, адрес которого пользователь указал в настройках. */
interface TorrServerRepository {
    /**
     * Добавляет раздачу на сервер и ждёт список файлов. [title] — название, под которым раздача видна в интерфейсе
     * TorrServer (пустое — сервер возьмёт своё). [onProgress] вызывается при каждом опросе, пока метаданных нет.
     * Ошибки сети и таймаут метаданных пробрасываются.
     */
    suspend fun addTorrent(link: String, title: String, onProgress: (TorrentProgress) -> Unit): TorrentContent

    /** Адрес HTTP-потока [file] раздачи [hash] и заголовки авторизации. */
    suspend fun streamFor(hash: String, file: TorrentFile): TorrentStream

    /**
     * Ждёт, пока TorrServer начнёт отдавать начало файла ([stream]): на старте раздачи он ищет пиров до минуты, а
     * плеер столько не ждёт и бросает соединение. `true` — данные пошли; `false` — за отведённое время не пришли.
     */
    suspend fun awaitStreamData(stream: TorrentStream): Boolean

    /**
     * Инфо-хеши раздач, которые сейчас есть на сервере (hex, нижний регистр). Только справка для списка выбора, поэтому
     * при любой ошибке сети или разбора возвращает пустое множество.
     */
    suspend fun knownHashes(): Set<String>
}
