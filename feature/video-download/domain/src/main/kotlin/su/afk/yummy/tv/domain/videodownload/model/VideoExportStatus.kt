package su.afk.yummy.tv.domain.videodownload.model

enum class VideoExportStatus {
    Idle,
    Queued,
    Preparing,
    Copying,
    Exported,
    Failed,
}
