package su.afk.yummy.tv.domain.videodownload.model

sealed interface VideoDownloadStreamRefreshResult {
    data class Success(
        val stream: VideoDownloadRestartStream,
    ) : VideoDownloadStreamRefreshResult

    data class Failure(
        val message: String,
        /** Техническая причина для диагностики (тип результата резолвера, HTTP-код); пользователю не показывается. */
        val diagnostic: String? = null,
        val cause: Throwable? = null,
    ) : VideoDownloadStreamRefreshResult
}
