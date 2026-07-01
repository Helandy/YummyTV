package su.afk.yummy.tv.domain.videodownload.usecase

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadQualityOption
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRequest
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
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
    ): List<VideoDownloadQualityOption> {
        val mapped = qualityMap
            ?.filter { (label, url) -> label.isNotBlank() && url.isNotBlank() }
            ?.map { (label, url) -> VideoDownloadQualityOption(label = label, url = url) }
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

class GetVideoDownloadUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke(id: Long): VideoDownloadItem? = repository.getDownload(id)
}
