package su.afk.yummy.tv.data.account.mapper

import su.afk.yummy.tv.data.account.dto.video.YaniPostVideoItemDto
import su.afk.yummy.tv.domain.account.model.VideoWatchSyncItem

internal fun VideoWatchSyncItem.toVideoItemDto(): YaniPostVideoItemDto =
    YaniPostVideoItemDto(
        videoId = videoId,
        time = timeSeconds,
        date = dateSeconds,
    )
