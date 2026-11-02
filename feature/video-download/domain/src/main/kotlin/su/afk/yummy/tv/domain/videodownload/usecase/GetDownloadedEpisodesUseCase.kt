package su.afk.yummy.tv.domain.videodownload.usecase

import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.common.episode.episodeNumberOrNull
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import javax.inject.Inject

/**
 * Возвращает полностью скачанные серии тайтла в озвучке [dubbing], по порядку номеров серий.
 * Нужен офлайн-плееру, чтобы переходить на следующую скачанную серию.
 */
class GetDownloadedEpisodesUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke(animeId: Int, dubbing: String): List<VideoDownloadItem> {
        if (animeId <= 0) return emptyList()
        return repository.observeDownloads().first()
            .filter { item ->
                item.status == VideoDownloadStatus.Downloaded &&
                    item.animeId == animeId &&
                    item.dubbing.equals(dubbing, ignoreCase = true)
            }
            .sortedByDescending { it.updatedAt }
            .distinctBy { it.episode.episodeGroupKey() }
            .sortedWith(
                compareBy<VideoDownloadItem> { it.episode.episodeNumberOrNull() ?: Double.MAX_VALUE }
                    .thenBy { it.episode },
            )
    }
}
