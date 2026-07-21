package su.afk.yummy.tv.domain.videodownload.model

sealed interface VideoDownloadStreamRefreshResult {
    data class Success(
        val stream: VideoDownloadRestartStream,
    ) : VideoDownloadStreamRefreshResult

    data class Failure(
        val message: String,
    ) : VideoDownloadStreamRefreshResult
}
