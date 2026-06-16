package su.afk.yummy.tv.core.tv

import android.content.ContentValues
import android.content.Context
import androidx.tvprovider.media.tv.TvContractCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.core.utils.KodikThumbnailExtractor
import javax.inject.Inject
import javax.inject.Singleton

// TvContractCompat.WatchNextProgramColumns / ProgramColumns are @RestrictTo — raw column names.
private const val COLUMN_WATCH_NEXT_TYPE = "watch_next_type"
private const val COLUMN_TYPE = "type"
private const val COLUMN_TITLE = "title"
private const val COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS = "last_engagement_time_utc_millis"
private const val COLUMN_LAST_PLAYBACK_POSITION_MILLIS = "last_playback_position_millis"
private const val COLUMN_DURATION_MILLIS = "duration_millis"
private const val COLUMN_INTERNAL_PROVIDER_ID = "internal_provider_id"
private const val COLUMN_POSTER_ART_URI = "poster_art_uri"
private const val COLUMN_INTENT_URI = "intent_uri"

@Singleton
internal class WatchNextManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val resolver = context.contentResolver

    suspend fun sync(entries: List<WatchProgressEntry>) {
        deleteAll()
        WatchProgressStore.latestByAnime(
            entries.filter { it.animeId > 0 }
        )
            .forEach { entry ->
                val episodeThumbnail = resolveEpisodeThumbnail(entry)
                insert(entry, episodeThumbnail)
            }
    }

    private fun insert(entry: WatchProgressEntry, episodeThumbnail: String?) {
        val artUri = episodeThumbnail
            ?: entry.screenshotUrl.takeIf { it.isLikelyImageUrl() }
            ?: entry.posterUrl.ifBlank { null }
        val values = ContentValues().apply {
            put(COLUMN_WATCH_NEXT_TYPE, TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
            put(COLUMN_TYPE, TvContractCompat.PreviewPrograms.TYPE_TV_SERIES)
            put(COLUMN_TITLE, entry.animeTitle)
            put(COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS, entry.updatedAt)
            put(COLUMN_LAST_PLAYBACK_POSITION_MILLIS, entry.positionMs.toInt())
            put(COLUMN_DURATION_MILLIS, entry.durationMs.toInt())
            put(COLUMN_INTERNAL_PROVIDER_ID, entry.episodeUrl)
            if (artUri != null) put(COLUMN_POSTER_ART_URI, artUri)
            put(COLUMN_INTENT_URI, "yummytv://details/${entry.animeId}")
        }
        runCatching {
            resolver.insert(TvContractCompat.WatchNextPrograms.CONTENT_URI, values)
        }
    }

    private fun deleteAll() {
        runCatching {
            resolver.delete(TvContractCompat.WatchNextPrograms.CONTENT_URI, null, null)
        }
    }

    private suspend fun resolveEpisodeThumbnail(entry: WatchProgressEntry): String? {
        val screenshotSource = entry.screenshotUrl.takeIf { it.isKodikSourceUrl() }
        return screenshotSource?.let { KodikThumbnailExtractor.extract(it) }
            ?: entry.episodeUrl.takeIf { it.isNotBlank() }
                ?.let { KodikThumbnailExtractor.extract(it) }
    }
}

private fun String.isKodikSourceUrl(): Boolean = contains("kodik", ignoreCase = true)

private fun String.isLikelyImageUrl(): Boolean =
    Regex("""\.(webp|avif|jpe?g|png)(\?.*)?$""", RegexOption.IGNORE_CASE).containsMatchIn(this)
