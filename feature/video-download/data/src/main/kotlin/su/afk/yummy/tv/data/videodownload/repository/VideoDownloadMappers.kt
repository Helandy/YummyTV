package su.afk.yummy.tv.data.videodownload.repository

import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.storage.videodownload.VideoDownloadEntry
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRequest
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus

private val videoDownloadJson = Json { ignoreUnknownKeys = true }

internal fun VideoDownloadEntry.toDomain(): VideoDownloadItem =
    VideoDownloadItem(
        id = id,
        animeId = animeId,
        animeTitle = animeTitle,
        posterUrl = posterUrl,
        episode = episode,
        videoId = videoId,
        playerName = playerName,
        playerId = playerId,
        dubbing = dubbing,
        iframeUrl = iframeUrl,
        screenshotUrl = screenshotUrl,
        qualityLabel = qualityLabel,
        streamUrl = streamUrl,
        headers = runCatching { videoDownloadJson.decodeFromString<Map<String, String>>(headersJson) }
            .getOrDefault(emptyMap()),
        cacheKey = cacheKey,
        status = status.toStatus(),
        progress = progress,
        bytesDownloaded = bytesDownloaded,
        totalBytes = totalBytes,
        errorMessage = errorMessage,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun VideoDownloadRequest.toEntry(now: Long): VideoDownloadEntry =
    VideoDownloadEntry(
        animeId = animeId,
        animeTitle = animeTitle,
        posterUrl = posterUrl,
        episode = episode,
        videoId = videoId,
        playerName = playerName,
        playerId = playerId,
        dubbing = dubbing,
        iframeUrl = iframeUrl,
        screenshotUrl = screenshotUrl,
        qualityLabel = quality.label,
        streamUrl = quality.url,
        headersJson = videoDownloadJson.encodeToString(headers),
        cacheKey = buildCacheKey(this),
        status = VideoDownloadStatus.Queued.name,
        progress = 0f,
        bytesDownloaded = 0L,
        totalBytes = null,
        errorMessage = null,
        createdAt = now,
        updatedAt = now,
    )

internal fun Map<String, String>.toVideoDownloadHeadersJson(): String =
    videoDownloadJson.encodeToString(this)

internal fun VideoDownloadStatus.storageName(): String = name

private fun String.toStatus(): VideoDownloadStatus =
    runCatching { VideoDownloadStatus.valueOf(this) }.getOrDefault(VideoDownloadStatus.Failed)

private fun buildCacheKey(request: VideoDownloadRequest): String =
    listOf(
        request.animeId.toString(),
        request.videoId.toString(),
        request.playerId?.toString().orEmpty(),
        request.iframeUrl,
        request.quality.label,
    ).joinToString(separator = "|")
