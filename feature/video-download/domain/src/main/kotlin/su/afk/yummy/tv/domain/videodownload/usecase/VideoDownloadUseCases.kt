package su.afk.yummy.tv.domain.videodownload.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadQualityOption
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRequest
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRestartStream
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStreamRefreshResult
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadStreamRefresher
import javax.inject.Inject

class ObserveVideoDownloadsUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    operator fun invoke(): Flow<List<VideoDownloadItem>> = repository.observeDownloads()
}

class ObserveVideoDownloadStatusesUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    operator fun invoke(animeId: Int): Flow<Map<String, VideoDownloadItem>> =
        repository.observeStatuses(animeId)
}

class PrepareVideoDownloadQualityOptionsUseCase @Inject constructor() {
    operator fun invoke(
        streamUrl: String,
        qualityMap: LinkedHashMap<String, String>?,
        qualityHeaders: Map<String, Map<String, String>> = emptyMap(),
    ): List<VideoDownloadQualityOption> {
        val mapped = qualityMap
            ?.filter { (label, url) -> label.isNotBlank() && url.isNotBlank() }
            ?.map { (label, url) ->
                VideoDownloadQualityOption(
                    label = label,
                    url = url,
                    headers = qualityHeaders[label].orEmpty(),
                )
            }
            .orEmpty()
        return mapped.ifEmpty {
            listOf(VideoDownloadQualityOption(label = "Auto", url = streamUrl))
        }
    }
}

class EnqueueVideoDownloadUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke(request: VideoDownloadRequest): VideoDownloadItem =
        repository.enqueue(request)
}

class CancelOrDeleteVideoDownloadUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke(id: Long) {
        repository.cancelOrDelete(id)
    }
}

class PauseVideoDownloadUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke(id: Long) {
        repository.pause(id)
    }
}

class RestartVideoDownloadUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke(id: Long, stream: VideoDownloadRestartStream? = null) {
        repository.restart(id, stream)
    }
}

class RefreshVideoDownloadStreamUseCase @Inject constructor(
    private val refresher: VideoDownloadStreamRefresher,
) {
    suspend operator fun invoke(
        item: VideoDownloadItem,
        autoQualityLabel: String,
    ): VideoDownloadStreamRefreshResult = refresher.refresh(item, autoQualityLabel)
}

class GetVideoDownloadUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke(id: Long): VideoDownloadItem? = repository.getDownload(id)
}

class UpdateVideoDownloadPreparedStreamUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke(id: Long, stream: VideoDownloadRestartStream) {
        repository.updatePreparedStream(id, stream)
    }
}

class UpdateVideoDownloadStatusUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke(
        id: Long,
        status: VideoDownloadStatus,
        progress: Float? = null,
        bytesDownloaded: Long? = null,
        totalBytes: Long? = null,
        errorMessage: String? = null,
    ) {
        repository.updateStatus(
            id = id,
            status = status,
            progress = progress,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes,
            errorMessage = errorMessage,
        )
    }
}
