package su.afk.yummy.tv.domain.videodownload.usecase

import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import javax.inject.Inject

/**
 * Ищет полностью скачанную серию тайтла с той же озвучкой. Номера серий сравниваются по
 * [episodeGroupKey] («1» и «01» — одна серия). Возвращает null, если подходящей загрузки нет.
 */
class FindDownloadedEpisodeUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke(animeId: Int, episode: String, dubbing: String): VideoDownloadItem? {
        if (animeId <= 0) return null
        val episodeKey = episode.episodeGroupKey()
        return repository.observeDownloads().first().firstOrNull { item ->
            item.status == VideoDownloadStatus.Downloaded &&
                item.animeId == animeId &&
                item.episode.episodeGroupKey() == episodeKey &&
                item.dubbing.equals(dubbing, ignoreCase = true)
        }
    }
}
