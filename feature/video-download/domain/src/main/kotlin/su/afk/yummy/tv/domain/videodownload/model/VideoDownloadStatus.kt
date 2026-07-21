package su.afk.yummy.tv.domain.videodownload.model

enum class VideoDownloadStatus {
    Idle,
    Resolving,
    Queued,
    Downloading,
    Paused,
    Downloaded,
    Failed,
    Deleting,
    Deleted,
}
