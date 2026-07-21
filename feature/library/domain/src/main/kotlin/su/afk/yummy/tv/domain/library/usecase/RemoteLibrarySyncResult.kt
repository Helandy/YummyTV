package su.afk.yummy.tv.domain.library.usecase

sealed interface RemoteLibrarySyncResult {
    data class Success(val syncError: Throwable?) : RemoteLibrarySyncResult
    data class Failure(val error: Throwable) : RemoteLibrarySyncResult
}
