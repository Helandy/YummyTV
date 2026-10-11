package su.afk.yummy.tv.domain.videodownload.repository

import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStreamRefreshResult

interface VideoDownloadStreamRefreshRepository {
    /**
     * @param forceRefresh минует кэш резолва и просит источник выдать ссылки заново (для CVH — на
     *   резервном узле CDN): нужен после отказа узла, когда прежние ссылки вести не туда.
     */
    suspend fun refresh(
        item: VideoDownloadItem,
        autoQualityLabel: String,
        forceRefresh: Boolean = false,
    ): VideoDownloadStreamRefreshResult
}
