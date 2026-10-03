package su.afk.yummy.tv.domain.library.model

sealed interface RemoteLibrarySyncResult {
    data class Success(val syncError: Throwable?) : RemoteLibrarySyncResult
    data class Failure(val error: Throwable) : RemoteLibrarySyncResult
}
