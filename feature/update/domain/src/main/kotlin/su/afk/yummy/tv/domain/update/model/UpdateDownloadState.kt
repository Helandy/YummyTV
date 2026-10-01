package su.afk.yummy.tv.domain.update.model

import java.io.File

/** Состояние фоновой загрузки APK обновления. */
sealed interface UpdateDownloadState {
    /** Загрузка для этого обновления не запускалась или была отменена. */
    data object Idle : UpdateDownloadState

    data class Downloading(val progress: Float) : UpdateDownloadState

    data class Downloaded(val file: File) : UpdateDownloadState

    data class Failed(val message: String?) : UpdateDownloadState
}
