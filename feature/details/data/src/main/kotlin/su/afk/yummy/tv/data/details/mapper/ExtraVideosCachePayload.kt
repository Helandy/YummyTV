package su.afk.yummy.tv.data.details.mapper

import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeVideoSkipSegment
import su.afk.yummy.tv.core.model.anime.AnimeVideoSkips

/**
 * Конверт для кэша видео подключаемых модулей-источников: доменные [AnimeVideo] не сериализуемы,
 * поэтому в `document_cache` кладётся эта копия и разворачивается обратно через [toDomain].
 */
@Serializable
internal data class ExtraVideosCachePayload(
    val videos: List<ExtraVideoCacheItem> = emptyList(),
) {
    fun toDomain(): List<AnimeVideo> = videos.map { it.toDomain() }
}

@Serializable
internal data class ExtraVideoCacheItem(
    val id: Int,
    val episode: String,
    val dubbing: String,
    val player: String,
    val playerId: Int? = null,
    val iframeUrl: String,
    val durationSeconds: Int? = null,
    val views: Int? = null,
    val watchedEndTimeSeconds: Int? = null,
    val watchedDateSeconds: Long? = null,
    val isSubscribed: Boolean = false,
    val openingStartMs: Long? = null,
    val openingEndMs: Long? = null,
    val endingStartMs: Long? = null,
    val endingEndMs: Long? = null,
) {
    fun toDomain(): AnimeVideo = AnimeVideo(
        id = id,
        episode = episode,
        dubbing = dubbing,
        player = player,
        playerId = playerId,
        iframeUrl = iframeUrl,
        durationSeconds = durationSeconds,
        views = views,
        watchedEndTimeSeconds = watchedEndTimeSeconds,
        watchedDateSeconds = watchedDateSeconds,
        isSubscribed = isSubscribed,
        skips = AnimeVideoSkips(
            opening = segment(openingStartMs, openingEndMs),
            ending = segment(endingStartMs, endingEndMs),
        ),
    )
}

internal fun List<AnimeVideo>.toExtraVideosCachePayload(): ExtraVideosCachePayload =
    ExtraVideosCachePayload(map { it.toExtraVideoCacheItem() })

private fun AnimeVideo.toExtraVideoCacheItem(): ExtraVideoCacheItem = ExtraVideoCacheItem(
    id = id,
    episode = episode,
    dubbing = dubbing,
    player = player,
    playerId = playerId,
    iframeUrl = iframeUrl,
    durationSeconds = durationSeconds,
    views = views,
    watchedEndTimeSeconds = watchedEndTimeSeconds,
    watchedDateSeconds = watchedDateSeconds,
    isSubscribed = isSubscribed,
    openingStartMs = skips.opening?.startMs,
    openingEndMs = skips.opening?.endMs,
    endingStartMs = skips.ending?.startMs,
    endingEndMs = skips.ending?.endMs,
)

private fun segment(startMs: Long?, endMs: Long?): AnimeVideoSkipSegment? =
    if (startMs != null && endMs != null) AnimeVideoSkipSegment(startMs, endMs) else null
