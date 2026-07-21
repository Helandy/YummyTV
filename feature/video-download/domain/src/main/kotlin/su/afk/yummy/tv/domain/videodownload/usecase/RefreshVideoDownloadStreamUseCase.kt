package su.afk.yummy.tv.domain.videodownload.usecase

import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStreamRefreshResult
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadStreamRefresher
import javax.inject.Inject

/** Обновляет устаревшие данные потока перед возобновлением загрузки. */
class RefreshVideoDownloadStreamUseCase @Inject constructor(
    private val refresher: VideoDownloadStreamRefresher,
) {
    suspend operator fun invoke(
        item: VideoDownloadItem,
        autoQualityLabel: String,
    ): VideoDownloadStreamRefreshResult = refresher.refresh(item, autoQualityLabel)
}
