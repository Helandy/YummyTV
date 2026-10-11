package su.afk.yummy.tv.domain.videodownload.usecase

import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import javax.inject.Inject

/**
 * Включает или выключает режим «качать только по безлимитной сети» и сразу применяет его к уже
 * поставленным в очередь загрузкам: при включении они ждут Wi-Fi, при выключении стартуют по любой
 * доступной сети.
 */
class SetVideoDownloadWifiOnlyUseCase @Inject constructor(
    private val repository: VideoDownloadRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.setWifiOnly(enabled)
    }
}
