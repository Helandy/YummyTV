package su.afk.yummy.tv.domain.videodownload.repository

import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStreamRefreshResult

interface VideoDownloadStreamRefreshRepository {
    suspend fun refresh(
        item: VideoDownloadItem,
        autoQualityLabel: String,
    ): VideoDownloadStreamRefreshResult
}
