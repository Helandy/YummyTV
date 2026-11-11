package su.afk.yummy.tv.source.torrent.data.source

import android.content.Context
import android.text.format.Formatter
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.source.torrent.data.R
import javax.inject.Inject

internal class AndroidTorrentMessages @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : TorrentMessages {
    override val sourceName: String get() = context.getString(R.string.torrent_source_name)
    override val pickTorrentTitle: String get() = context.getString(R.string.torrent_pick_title)
    override val pickFileTitle: String get() = context.getString(R.string.torrent_pick_file_title)
    override val cancel: String get() = context.getString(R.string.torrent_cancel)
    override val notConfigured: String get() = context.getString(R.string.torrent_not_configured)
    override val nothingFound: String get() = context.getString(R.string.torrent_nothing_found)
    override val searchFailed: String get() = context.getString(R.string.torrent_search_failed)
    override val serverFailed: String get() = context.getString(R.string.torrent_server_failed)
    override val noVideoFiles: String get() = context.getString(R.string.torrent_no_video_files)
    override val cancelled: String get() = context.getString(R.string.torrent_cancelled)

    override fun torrentSubtitle(sizeBytes: Long, seeders: Int, tracker: String): String =
        listOfNotNull(
            sizeBytes.takeIf { it > 0 }?.let { Formatter.formatShortFileSize(context, it) },
            context.getString(R.string.torrent_seeders, seeders),
            tracker.takeIf { it.isNotBlank() },
        ).joinToString(" · ")

    override fun fileSubtitle(sizeBytes: Long): String =
        if (sizeBytes > 0) Formatter.formatShortFileSize(context, sizeBytes) else ""
}
