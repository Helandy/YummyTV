package su.afk.yummy.tv.feature.account.account.mapper

import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.domain.account.model.VideoWatchSyncItem

/** Локальный прогресс серии в формате синхронизации просмотров с сервером (секунды). */
internal fun AnimeWatchProgress.toVideoWatchSyncItem(): VideoWatchSyncItem =
    VideoWatchSyncItem(
        videoId = videoId,
        timeSeconds = (positionMs / 1000L).toInt(),
        dateSeconds = (updatedAt / 1000L).toInt(),
    )
