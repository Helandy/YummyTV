package su.afk.yummy.tv.source.torrent.data.source

import android.content.Context
import android.text.format.Formatter
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.core.designsystem.permissions.hasLocalNetworkAccess
import su.afk.yummy.tv.source.torrent.data.R
import javax.inject.Inject

internal class AndroidTorrentMessages @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : TorrentMessages {
    override val sourceName: String get() = context.getString(R.string.torrent_source_name)
    override val pickTorrentTitle: String get() = context.getString(R.string.torrent_pick_title)
    override val pickFileTitle: String get() = context.getString(R.string.torrent_pick_file_title)
    override val cancel: String get() = context.getString(R.string.torrent_cancel)
    override val stepSearching: String get() = context.getString(R.string.torrent_step_searching)
    override val stepFetchingFiles: String get() = context.getString(R.string.torrent_step_fetching_files)
    override val notConfigured: String get() = context.getString(R.string.torrent_not_configured)
    override val nothingFound: String get() = context.getString(R.string.torrent_nothing_found)
    override val searchFailed: String get() = withLocalNetworkHint(context.getString(R.string.torrent_search_failed))
    override val serverFailed: String get() = withLocalNetworkHint(context.getString(R.string.torrent_server_failed))
    override val noVideoFiles: String get() = context.getString(R.string.torrent_no_video_files)
    override val noPeers: String get() = context.getString(R.string.torrent_no_peers)
    override val streamNotStarting: String get() = context.getString(R.string.torrent_stream_not_starting)
    override val cancelled: String get() = context.getString(R.string.torrent_cancelled)

    /** Без доступа к локальной сети соединения с LAN-адресом висят до таймаута: подсказываем причину. */
    private fun withLocalNetworkHint(message: String): String =
        if (context.hasLocalNetworkAccess()) message else message + "\n" + context.getString(R.string.torrent_local_network_denied)

    override val retry: String get() = context.getString(R.string.torrent_retry)
    override fun stepFetchingFilesPeers(activePeers: Int, totalPeers: Int, trackerSeeders: Int): String =
        context.getString(R.string.torrent_step_fetching_files_peers, activePeers, totalPeers, trackerSeeders)

    override val hevcUnsupportedWarning: String get() = context.getString(R.string.torrent_warning_hevc_unsupported)
    override val inTorrServerBadge: String get() = context.getString(R.string.torrent_badge_in_torrserver)
    override val subtitlesBadge: String get() = context.getString(R.string.torrent_badge_subtitles)

    override fun sizeText(sizeBytes: Long): String =
        if (sizeBytes > 0) Formatter.formatShortFileSize(context, sizeBytes) else ""

    override fun seedersText(seeders: Int): String =
        context.resources.getQuantityString(R.plurals.torrent_seeders, seeders, seeders)

    override fun fileSubtitle(sizeBytes: Long): String =
        if (sizeBytes > 0) Formatter.formatShortFileSize(context, sizeBytes) else ""
}
