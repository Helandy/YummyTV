package su.afk.yummy.tv.core.tv

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.tvprovider.media.tv.Channel
import androidx.tvprovider.media.tv.TvContractCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "tv_provider_prefs"
private const val KEY_CHANNEL_ID = "preview_channel_id"

// TvContractCompat.ProgramColumns constants are @RestrictTo — use raw column names instead.
private const val PROGRAM_COLUMN_TITLE = "title"
private const val PROGRAM_COLUMN_POSTER_ART_URI = "poster_art_uri"

@Singleton
internal class PreviewChannelManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val resolver = context.contentResolver
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _browsableRequest = MutableSharedFlow<Long>(replay = 1)
    val browsableRequest: SharedFlow<Long> = _browsableRequest

    private val _isBrowsable = MutableStateFlow(false)
    val isBrowsable: StateFlow<Boolean> = _isBrowsable

    fun checkBrowsable() {
        val channelId = prefs.getLong(KEY_CHANNEL_ID, -1L)
        _isBrowsable.value = channelId != -1L && isChannelBrowsable(channelId)
    }

    fun requestBrowsable() {
        val channelId = getOrCreateChannel() ?: return
        _browsableRequest.tryEmit(channelId)
    }

    fun syncNewContent(items: List<HomeFeedItem>) {
        if (items.isEmpty()) return
        val channelId = getOrCreateChannel() ?: return

        runCatching {
            resolver.delete(
                TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
                null, null,
            )
        }

        items.take(20).forEach { item ->
            val animeId = when (val action = item.action) {
                is HomeFeedItemAction.OpenSeries -> action.seriesId
                is HomeFeedItemAction.OpenVideo -> action.videoId
                is HomeFeedItemAction.OpenCollection -> return@forEach
            }
            insertProgram(channelId, animeId, item)
        }
    }

    private fun insertProgram(channelId: Long, animeId: Int, item: HomeFeedItem) {
        val posterUrl = item.poster?.run { medium ?: big ?: fullsize ?: small }
        val values = ContentValues().apply {
            put(TvContractCompat.PreviewPrograms.COLUMN_CHANNEL_ID, channelId)
            put(TvContractCompat.PreviewPrograms.COLUMN_TYPE, TvContractCompat.PreviewPrograms.TYPE_TV_SERIES)
            put(PROGRAM_COLUMN_TITLE, item.title)
            put(TvContractCompat.PreviewPrograms.COLUMN_INTERNAL_PROVIDER_ID, animeId.toString())
            if (!posterUrl.isNullOrBlank()) put(PROGRAM_COLUMN_POSTER_ART_URI, posterUrl)
            put(TvContractCompat.PreviewPrograms.COLUMN_INTENT_URI, "yummytv://details/$animeId")
        }
        runCatching {
            resolver.insert(TvContractCompat.PreviewPrograms.CONTENT_URI, values)
        }
    }

    private fun getOrCreateChannel(): Long? {
        val saved = prefs.getLong(KEY_CHANNEL_ID, -1L)
        if (saved != -1L && channelExists(saved)) return saved

        prefs.edit { remove(KEY_CHANNEL_ID) }

        val channel = Channel.Builder()
            .setType(TvContractCompat.Channels.TYPE_PREVIEW)
            .setDisplayName(context.getString(R.string.tv_preview_channel_new))
            .setAppLinkIntentUri("yummytv://home".toUri())
            .build()

        val uri = runCatching {
            resolver.insert(TvContractCompat.Channels.CONTENT_URI, channel.toContentValues())
        }.getOrNull() ?: return null

        val channelId = ContentUris.parseId(uri)
        prefs.edit { putLong(KEY_CHANNEL_ID, channelId) }
        return channelId
    }

    private fun channelExists(channelId: Long): Boolean =
        runCatching {
            resolver.query(
                TvContractCompat.buildChannelUri(channelId),
                arrayOf(TvContractCompat.Channels._ID),
                null, null, null,
            )?.use { it.count > 0 } ?: false
        }.getOrDefault(false)

    private fun isChannelBrowsable(channelId: Long): Boolean =
        runCatching {
            resolver.query(
                TvContractCompat.buildChannelUri(channelId),
                arrayOf(TvContractCompat.Channels.COLUMN_BROWSABLE),
                null, null, null,
            )?.use { cursor ->
                cursor.moveToFirst() && cursor.getInt(0) == 1
            } ?: false
        }.getOrDefault(false)
}
