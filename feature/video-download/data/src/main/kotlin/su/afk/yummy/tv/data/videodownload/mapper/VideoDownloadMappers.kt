package su.afk.yummy.tv.data.videodownload.mapper

import kotlinx.serialization.json.Json
import su.afk.yummy.tv.core.storage.videodownload.VideoDownloadEntry
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadCacheKeyScheme
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRequest
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.model.VideoExportStatus

private val videoDownloadJson = Json

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
        cacheKeyScheme = VideoDownloadCacheKeyScheme.fromStorageValue(cacheKeyScheme),
        status = status.toVideoDownloadStatus(),
        progress = progress,
        bytesDownloaded = bytesDownloaded,
        totalBytes = totalBytes,
        errorMessage = errorMessage,
        exportStatus = exportStatus.toExportStatus(),
        exportProgress = exportProgress,
        exportDirectoryUri = exportDirectoryUri,
        exportedFileUri = exportedFileUri,
        exportErrorMessage = exportErrorMessage,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun VideoDownloadRequest.toEntry(
    now: Long,
    cacheKeyScheme: VideoDownloadCacheKeyScheme,
): VideoDownloadEntry =
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
        cacheKeyScheme = cacheKeyScheme.storageValue,
        status = VideoDownloadStatus.Queued.name,
        progress = 0f,
        bytesDownloaded = 0L,
        totalBytes = null,
        errorMessage = null,
        exportStatus = VideoExportStatus.Idle.name,
        exportProgress = 0f,
        exportDirectoryUri = null,
        exportedFileUri = null,
        exportErrorMessage = null,
        createdAt = now,
        updatedAt = now,
    )

internal fun Map<String, String>.toVideoDownloadHeadersJson(): String =
    videoDownloadJson.encodeToString(this)

internal fun VideoDownloadStatus.storageName(): String = name

internal fun String.toVideoDownloadStatus(): VideoDownloadStatus =
    runCatching { VideoDownloadStatus.valueOf(this) }.getOrDefault(VideoDownloadStatus.Failed)

private fun String.toExportStatus(): VideoExportStatus =
    runCatching { VideoExportStatus.valueOf(this) }.getOrDefault(VideoExportStatus.Idle)

private fun buildCacheKey(request: VideoDownloadRequest): String =
    listOf(
        request.animeId.toString(),
        request.videoId.toString(),
        request.playerId?.toString().orEmpty(),
        request.iframeUrl,
        request.quality.label,
    ).joinToString(separator = "|")
