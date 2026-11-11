package su.afk.yummy.tv.source.torrent.data.source

import su.afk.yummy.tv.source.torrent.domain.model.TorrentSelection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Передаёт выбор пользователя от `confirmOpen` (деталка) к `resolve` (плеер): выбор делается до перехода,
 * а ссылку на поток плеер запрашивает отдельно. Хранится недолго, чтобы повторный вход и смена серии
 * не заставляли выбирать раздачу заново, а устаревший выбор не попадал в чужую серию.
 */
@Singleton
internal class TorrentSelectionHolder @Inject constructor() {

    private data class Entry(val url: String, val selection: TorrentSelection, val atMs: Long)

    @Volatile
    private var last: Entry? = null

    fun put(url: String, selection: TorrentSelection, nowMs: Long = System.currentTimeMillis()) {
        last = Entry(url, selection, nowMs)
    }

    fun get(url: String, nowMs: Long = System.currentTimeMillis()): TorrentSelection? =
        last?.takeIf { it.url == url && nowMs - it.atMs < REUSE_WINDOW_MS }?.selection

    private companion object {
        const val REUSE_WINDOW_MS = 15 * 60_000L
    }
}
