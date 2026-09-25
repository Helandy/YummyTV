package su.afk.yummy.tv.feature.videodownload.utils

import su.afk.yummy.tv.domain.videodownload.model.VideoExportStatus

/** Экспорт ещё идёт: в очереди, готовится или копируется. */
internal val VideoExportStatus.isActive: Boolean
    get() = this == VideoExportStatus.Queued ||
        this == VideoExportStatus.Preparing ||
        this == VideoExportStatus.Copying
