package su.afk.yummy.tv.source.torrent.data.source

import su.afk.yummy.tv.source.torrent.domain.model.TorrentResult
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

    private data class Entry(
        val url: String,
        val selection: TorrentSelection,
        val atMs: Long,
        val startedAtMs: Long? = null,
    )

    private data class RememberedTorrent(val animeId: Int, val torrent: TorrentResult, val atMs: Long)

    @Volatile
    private var last: Entry? = null

    @Volatile
    private var lastTorrent: RememberedTorrent? = null

    /** Раздача, выбранная для тайтла: следующая/предыдущая серия берётся из неё без повторного диалога. */
    fun rememberTorrent(animeId: Int, torrent: TorrentResult, nowMs: Long = System.currentTimeMillis()) {
        lastTorrent = RememberedTorrent(animeId, torrent, nowMs)
    }

    /** Раздача, выбранная для тайтла (если она ещё не устарела). */
    fun torrentFor(animeId: Int, nowMs: Long = System.currentTimeMillis()): TorrentResult? =
        lastTorrent?.takeIf { it.animeId == animeId && nowMs - it.atMs < REUSE_WINDOW_MS }?.torrent

    /**
     * Поток по выбору запущен: ещё [RESTART_WINDOW_MS] плеер может перезапросить ту же серию (повтор после ошибки), и
     * выбор не спрашивается заново; позже вход в серию снова покажет список раздач.
     */
    fun markStarted(url: String, nowMs: Long = System.currentTimeMillis()) {
        last = last?.takeIf { it.url == url }?.copy(startedAtMs = nowMs) ?: return
    }

    /** Забывает выбор для [url]; запомненная раздача тайтла остаётся: список покажет её первой. */
    fun forget(url: String) {
        if (last?.url == url) last = null
    }

    fun put(url: String, selection: TorrentSelection, nowMs: Long = System.currentTimeMillis()) {
        last = Entry(url, selection, nowMs)
    }

    fun get(url: String, nowMs: Long = System.currentTimeMillis()): TorrentSelection? =
        last?.takeIf { it.url == url && it.isFresh(nowMs) }?.selection

    private fun Entry.isFresh(nowMs: Long): Boolean =
        if (startedAtMs != null) nowMs - startedAtMs < RESTART_WINDOW_MS else nowMs - atMs < REUSE_WINDOW_MS

    private companion object {
        const val REUSE_WINDOW_MS = 15 * 60_000L
        const val RESTART_WINDOW_MS = 30_000L
    }
}
