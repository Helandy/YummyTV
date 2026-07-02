package su.afk.yummy.tv.data.videodownload.worker.utils

import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem

internal fun VideoDownloadItem.isAllohaDownload(): Boolean =
    iframeUrl.contains(ALLOHA_MATCHER, ignoreCase = true) ||
            playerName.contains(ALLOHA_MATCHER, ignoreCase = true)

internal fun VideoDownloadItem.shouldRefreshBeforeDownload(runAttemptCount: Int): Boolean =
    streamUrl.streamKind().isAdaptive &&
            isAllohaDownload() &&
            (progress > 0f || runAttemptCount > 0 || errorMessage != null)

private const val ALLOHA_MATCHER = "alloha"
